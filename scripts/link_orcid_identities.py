"""Create Keycloak accounts for Rogue Scholar users and attach their ORCID identity.

Reads a CSV exported from the InvenioRDM database (columns: email, orcid and
optionally email_verified) and, for every row:

* links the ORCID identity to the existing Keycloak account with that email, or
* creates an account first when no Keycloak user has that email.

Defaults to a dry run that only reports what it would do. Pass --apply to write.

Authenticate either as an admin user or as a client's service account:

    KEYCLOAK_URL             e.g. https://auth.front-matter.de
    KEYCLOAK_REALM           realm the users live in, e.g. main
    KEYCLOAK_ADMIN_REALM     realm to authenticate against, defaults to master

    KEYCLOAK_ADMIN_USER      admin account
    KEYCLOAK_ADMIN_PASSWORD  its password

    KEYCLOAK_CLIENT_ID       client id, defaults to admin-cli
    KEYCLOAK_CLIENT_SECRET   set this to use the client's service account instead

A service account needs the realm-management role `manage-users` and, since
Keycloak 26.8.2, the `kc-admin-api` client scope, or the API answers 401.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import secrets
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import structlog

log = structlog.get_logger().bind(service="link-orcid-identities")

# Crockford Base32 without i, l, o and u, matching auto-username's generator.
USERNAME_ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"
USERNAME_PREFIX = "usr_"
USERNAME_LENGTH = 8
USERNAME_ATTEMPTS = 5
TOKEN_EXPIRY_MARGIN = 30.0


class KeycloakError(RuntimeError):
    """The Keycloak Admin API returned an unexpected response."""


class AuthError(RuntimeError):
    """Admin credentials were rejected. Never retried per row."""


class ConfigError(RuntimeError):
    """Required configuration is missing or invalid."""


@dataclass(frozen=True)
class Config:
    """Connection details for one Keycloak realm.

    Authenticates either as an admin user (password grant) or as a client's
    service account (client credentials), depending on which is configured.
    """

    server: str
    realm: str
    admin_realm: str
    client_id: str
    admin_user: str | None = None
    admin_password: str | None = None
    client_secret: str | None = None

    @property
    def uses_service_account(self) -> bool:
        return self.client_secret is not None

    @property
    def principal(self) -> str:
        """How to describe whoever we authenticate as, for log messages."""
        return (
            f"service account of {self.client_id!r}"
            if self.uses_service_account
            else repr(self.admin_user)
        )


@dataclass(frozen=True)
class Row:
    """One ORCID identity to restore, as exported from InvenioRDM."""

    email: str
    orcid: str
    email_verified: bool


@dataclass
class Report:
    """What the run did, or would do in a dry run."""

    created: int = 0
    linked_existing: int = 0
    already_linked: int = 0
    conflicts: list[str] = field(default_factory=list)
    skipped: list[str] = field(default_factory=list)


def config_from_env(env: dict[str, str]) -> Config:
    """Build a Config from environment variables, raising if any are missing."""
    missing = [
        name for name in ("KEYCLOAK_URL", "KEYCLOAK_REALM") if not env.get(name)
    ]
    if missing:
        raise ConfigError(f"missing environment variables: {', '.join(missing)}")

    secret = env.get("KEYCLOAK_CLIENT_SECRET")
    user = env.get("KEYCLOAK_ADMIN_USER")
    password = env.get("KEYCLOAK_ADMIN_PASSWORD")

    if secret:
        if not env.get("KEYCLOAK_CLIENT_ID"):
            raise ConfigError(
                "KEYCLOAK_CLIENT_SECRET needs KEYCLOAK_CLIENT_ID naming its client"
            )
    elif not (user and password):
        raise ConfigError(
            "set KEYCLOAK_ADMIN_USER and KEYCLOAK_ADMIN_PASSWORD, or "
            "KEYCLOAK_CLIENT_ID and KEYCLOAK_CLIENT_SECRET for a service account"
        )

    return Config(
        server=env["KEYCLOAK_URL"].rstrip("/"),
        realm=env["KEYCLOAK_REALM"],
        admin_realm=env.get("KEYCLOAK_ADMIN_REALM", "master"),
        client_id=env.get("KEYCLOAK_CLIENT_ID", "admin-cli"),
        admin_user=user,
        admin_password=password,
        client_secret=secret,
    )


def normalise_orcid(value: str) -> str:
    """Strip an https://orcid.org/ prefix, leaving the bare iD ORCID sends as sub."""
    bare = value.strip()
    for prefix in ("https://orcid.org/", "http://orcid.org/"):
        if bare.startswith(prefix):
            return bare[len(prefix) :]
    return bare


def parse_bool(value: str | None) -> bool:
    """Read the many spellings of true a CSV export may contain."""
    return (value or "").strip().lower() in {"t", "true", "1", "yes", "y"}


def read_rows(path: Path) -> list[Row]:
    """Read the export, dropping incomplete rows and duplicate ORCID iDs."""
    rows: list[Row] = []
    seen: set[str] = set()

    with path.open(newline="", encoding="utf-8") as handle:
        for number, record in enumerate(csv.DictReader(handle), start=2):
            email = (record.get("email") or "").strip().lower()
            orcid = normalise_orcid(record.get("orcid") or "")

            if not email or not orcid:
                log.warning("csv.row_incomplete", line=number, email=email)
                continue
            if orcid in seen:
                log.warning("csv.duplicate_orcid", line=number, orcid=orcid)
                continue

            seen.add(orcid)
            rows.append(
                Row(
                    email=email,
                    orcid=orcid,
                    email_verified=parse_bool(record.get("email_verified")),
                )
            )

    return rows


class KeycloakAdmin:
    """Thin Admin REST client that refreshes its token as needed."""

    def __init__(self, config: Config) -> None:
        self._config = config
        self._token: str | None = None
        self._expires_at = 0.0

    def _authenticate(self) -> None:
        config = self._config
        url = (
            f"{config.server}/realms/{config.admin_realm}"
            "/protocol/openid-connect/token"
        )
        if config.uses_service_account:
            form = {
                "client_id": config.client_id,
                "client_secret": config.client_secret,
                "grant_type": "client_credentials",
            }
        else:
            form = {
                "client_id": config.client_id,
                "username": config.admin_user,
                "password": config.admin_password,
                "grant_type": "password",
            }
        body = urllib.parse.urlencode(form).encode()
        request = urllib.request.Request(url, data=body, method="POST")
        request.add_header("Content-Type", "application/x-www-form-urlencoded")

        try:
            with urllib.request.urlopen(request) as response:
                payload = json.load(response)
        except urllib.error.HTTPError as error:
            detail = error.read().decode(errors="replace")[:200]
            raise AuthError(
                f"admin login failed ({error.code}) as {config.principal} "
                f"in realm {config.admin_realm!r}: {detail}"
            ) from error

        self._token = payload["access_token"]
        self._expires_at = time.monotonic() + float(payload.get("expires_in", 60))
        log.debug("admin.authenticated", realm=config.admin_realm)

    def _access_token(self) -> str:
        if self._token is None or time.monotonic() > self._expires_at - (
            TOKEN_EXPIRY_MARGIN
        ):
            self._authenticate()
        assert self._token is not None
        return self._token

    def request(
        self, method: str, path: str, body: dict[str, Any] | None = None
    ) -> tuple[int, dict[str, str], Any]:
        """Call the Admin API, returning status, headers and decoded JSON body."""
        url = f"{self._config.server}/admin/realms/{self._config.realm}{path}"
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(url, data=data, method=method)
        request.add_header("Authorization", f"Bearer {self._access_token()}")
        if data is not None:
            request.add_header("Content-Type", "application/json")

        try:
            with urllib.request.urlopen(request) as response:
                raw = response.read()
                parsed = json.loads(raw) if raw else None
                return response.status, dict(response.headers), parsed
        except urllib.error.HTTPError as error:
            if error.code in {404, 409}:
                return error.code, dict(error.headers), None
            detail = error.read().decode(errors="replace")[:200]
            raise KeycloakError(f"{method} {path} failed: {error.code} {detail}")

    def check_access(self) -> None:
        """Authenticate and confirm the admin can read this realm's users."""
        self.request("GET", "/users?max=1")
        log.debug("admin.access_ok", realm=self._config.realm)

    def find_user_by_email(self, email: str) -> dict[str, Any] | None:
        """Return the single user with this exact email, or None."""
        query = urllib.parse.urlencode({"email": email, "exact": "true"})
        _, _, users = self.request("GET", f"/users?{query}")
        if not users:
            return None
        if len(users) > 1:
            raise KeycloakError(f"{len(users)} users share the email {email}")
        return users[0]

    def find_user_by_orcid(self, alias: str, orcid: str) -> dict[str, Any] | None:
        """Return the user already linked to this ORCID iD, or None."""
        query = urllib.parse.urlencode({"idpAlias": alias, "idpUserId": orcid})
        _, _, users = self.request("GET", f"/users?{query}")
        return users[0] if users else None

    def username_available(self, username: str) -> bool:
        """Check whether a username is free in this realm."""
        query = urllib.parse.urlencode({"username": username, "exact": "true"})
        _, _, users = self.request("GET", f"/users?{query}")
        return not users

    def create_user(self, username: str, email: str, verified: bool) -> str:
        """Create an enabled user and return its id, taken from the Location header."""
        status, headers, _ = self.request(
            "POST",
            "/users",
            {
                "username": username,
                "email": email,
                "emailVerified": verified,
                "enabled": True,
            },
        )
        if status == 409:
            raise KeycloakError(f"user already exists: {username} / {email}")

        location = headers.get("Location", "")
        user_id = location.rstrip("/").rsplit("/", 1)[-1]
        if not user_id:
            raise KeycloakError(f"no id returned when creating {username}")
        return user_id

    def link_identity(self, user_id: str, alias: str, orcid: str) -> bool:
        """Attach an ORCID identity. Returns False when the link already existed."""
        status, _, _ = self.request(
            "POST",
            f"/users/{user_id}/federated-identity/{alias}",
            {"identityProvider": alias, "userId": orcid, "userName": orcid},
        )
        return status != 409


def generate_username(client: KeycloakAdmin) -> str:
    """Generate a free usr_xxxxxxxx username, matching the auto-username mapper."""
    for _ in range(USERNAME_ATTEMPTS):
        suffix = "".join(
            secrets.choice(USERNAME_ALPHABET) for _ in range(USERNAME_LENGTH)
        )
        username = f"{USERNAME_PREFIX}{suffix}"
        if client.username_available(username):
            return username
    raise KeycloakError(f"no free username after {USERNAME_ATTEMPTS} attempts")


def process_row(
    client: KeycloakAdmin, row: Row, alias: str, apply: bool, report: Report
) -> None:
    """Link one ORCID identity, creating the account when it does not exist yet."""
    entry = log.bind(orcid=row.orcid, email=row.email)

    linked = client.find_user_by_orcid(alias, row.orcid)
    existing = client.find_user_by_email(row.email)

    if linked is not None:
        if existing is not None and linked["id"] != existing["id"]:
            report.conflicts.append(
                f"{row.orcid}: linked to {linked['username']}, "
                f"email belongs to {existing['username']}"
            )
            entry.warning("row.conflict", linked_to=linked["username"])
            return
        report.already_linked += 1
        entry.debug("row.already_linked", username=linked["username"])
        return

    if existing is not None:
        report.linked_existing += 1
        entry.info("row.link_existing", username=existing["username"], apply=apply)
        if apply:
            client.link_identity(existing["id"], alias, row.orcid)
        return

    report.created += 1
    entry.info("row.create_user", apply=apply)
    if apply:
        username = generate_username(client)
        user_id = client.create_user(username, row.email, row.email_verified)
        client.link_identity(user_id, alias, row.orcid)
        entry.info("row.created", username=username, user_id=user_id)


def run(client: KeycloakAdmin, rows: list[Row], alias: str, apply: bool) -> Report:
    """Process every row, collecting failures rather than stopping at the first."""
    report = Report()

    for row in rows:
        try:
            process_row(client, row, alias, apply, report)
        except KeycloakError as error:
            report.skipped.append(f"{row.orcid} ({row.email}): {error}")
            log.error("row.failed", orcid=row.orcid, email=row.email, error=str(error))

    return report


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("csv", type=Path, help="CSV with email, orcid columns")
    parser.add_argument(
        "--provider-alias",
        default="orcid",
        help="identity provider alias in Keycloak (default: orcid)",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="write to Keycloak; without it the run only reports",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)

    try:
        config = config_from_env(dict(os.environ))
    except ConfigError as error:
        log.error("config.invalid", error=str(error))
        return 2

    rows = read_rows(args.csv)
    log.info(
        "run.start",
        rows=len(rows),
        realm=config.realm,
        alias=args.provider_alias,
        mode="apply" if args.apply else "dry-run",
    )

    client = KeycloakAdmin(config)
    # Fail fast on bad credentials rather than once per row.
    try:
        client.check_access()
    except (AuthError, KeycloakError) as error:
        log.error("admin.access_denied", error=str(error))
        return 2

    try:
        report = run(client, rows, args.provider_alias, args.apply)
    except AuthError as error:
        log.error("run.aborted", error=str(error))
        return 2
    except KeycloakError as error:
        log.error("run.aborted", error=str(error))
        return 1

    log.info(
        "run.finished",
        created=report.created,
        linked_existing=report.linked_existing,
        already_linked=report.already_linked,
        conflicts=len(report.conflicts),
        skipped=len(report.skipped),
        mode="apply" if args.apply else "dry-run",
    )
    for conflict in report.conflicts:
        log.warning("conflict", detail=conflict)
    for failure in report.skipped:
        log.warning("skipped", detail=failure)

    return 1 if report.skipped else 0


if __name__ == "__main__":
    raise SystemExit(main())
