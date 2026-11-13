# ORCID identity migration

Tooling and notes for restoring ORCID identity links in Keycloak, written during the
November 2026 migration away from the `keycloak-orcid` extension.

Replacing that extension with Keycloak's built-in **OpenID Connect v1.0** provider means
deleting and recreating the identity provider, because `providerId` is immutable since
Keycloak 26.8.1. Deleting an identity provider **also deletes every user's federated
identity link to it**. Users are then unknown to the provider: at the next ORCID sign-in
Keycloak runs the first broker login flow and creates a second, empty account instead of
recognising the existing one.

This directory holds what is needed to repair that, and to onboard users who exist in
InvenioRDM but not yet in Keycloak.

## The two ORCID formats

They are not interchangeable, and mixing them fails silently:

| Where | Format | Example |
| --- | --- | --- |
| `federated_identity.federated_user_id` | bare iD — ORCID's OIDC `sub` claim | `0000-0003-1419-2405` |
| `orcid` user attribute | URI, written by the old extension | `https://orcid.org/0000-0003-1419-2405` |

InvenioRDM reads the URI form from the attribute, so leave the attribute alone. A link
stored in URI form will never match at sign-in, and the user silently gets a new account.

For the same reason, do **not** add an "Attribute Importer" mapper for `sub` → `orcid`:
it writes the bare form, and with sync mode *force* it overwrites every user's attribute.

## Recovering links from Keycloak's own data

Keycloak already knows most users' ORCID iDs through the `orcid` attribute the old
extension wrote. That is the best source, and needs no export from anywhere:

```sql
-- how many users have the attribute but no link?
WITH r AS (SELECT id FROM realm WHERE name = 'main')
SELECT count(DISTINCT u.id)                                  AS users_with_attribute,
       count(DISTINCT u.id) FILTER (WHERE f.user_id IS NULL) AS users_missing_link
FROM user_attribute a
JOIN user_entity u ON u.id = a.user_id AND u.realm_id = (SELECT id FROM r)
LEFT JOIN federated_identity f ON f.user_id = u.id AND f.identity_provider = 'orcid'
WHERE a.name = 'orcid';

-- does any user carry two different ORCID iDs? resolve those by hand first
WITH r AS (SELECT id FROM realm WHERE name = 'main')
SELECT u.username, u.email,
       array_agg(DISTINCT regexp_replace(a.value, '^https?://orcid\.org/', '')) AS orcids
FROM user_attribute a
JOIN user_entity u ON u.id = a.user_id AND u.realm_id = (SELECT id FROM r)
WHERE a.name = 'orcid'
GROUP BY u.username, u.email
HAVING count(DISTINCT regexp_replace(a.value, '^https?://orcid\.org/', '')) > 1;

-- restore the missing links, bare form, one per user
INSERT INTO federated_identity
  (identity_provider, realm_id, federated_user_id, federated_username, token, user_id)
SELECT DISTINCT ON (u.id)
       'orcid', u.realm_id,
       regexp_replace(a.value, '^https?://orcid\.org/', ''),
       regexp_replace(a.value, '^https?://orcid\.org/', ''),
       NULL, u.id
FROM user_attribute a
JOIN user_entity u ON u.id = a.user_id
                  AND u.realm_id = (SELECT id FROM realm WHERE name = 'main')
WHERE a.name = 'orcid'
  AND NOT EXISTS (
    SELECT 1 FROM federated_identity f
    WHERE f.user_id = u.id AND f.identity_provider = 'orcid')
ORDER BY u.id, a.value;
```

`DISTINCT ON` matters: the attribute is multivalued, and two rows for one user would
collide on the `(identity_provider, user_id)` primary key.

Afterwards, check that no ORCID iD points at two accounts. Nothing in the schema prevents
it, and sign-in becomes ambiguous:

```sql
SELECT federated_user_id, count(*)
FROM federated_identity
WHERE identity_provider = 'orcid'
  AND realm_id = (SELECT id FROM realm WHERE name = 'main')
GROUP BY 1 HAVING count(*) > 1;
```

Any duplicate is usually a real account plus an empty one created by a sign-in made while
links were missing. Delete the empty account **through the admin console or REST**, never
by SQL — a user row is referenced by attributes, role mappings, credentials, required
actions and consents.

Keycloak caches users in memory, so clear the cache after writing SQL directly:
`POST /admin/realms/main/clear-user-cache` (204), or restart the service.

## Onboarding users who have no Keycloak account

Users who signed in with ORCID before Keycloak existed have an ORCID identity in
InvenioRDM but no Keycloak account. `link_orcid_identities.py` links or creates them.

Export the mapping from the InvenioRDM database — the table is `accounts_useridentity`
(`oauthclient_useridentity` is a pre-migration leftover and may hold fewer rows):

```sql
\copy (SELECT lower(trim(u.email)) AS email, \
              i.id AS orcid, \
              (u.confirmed_at IS NOT NULL) AS email_verified \
       FROM accounts_useridentity i \
       JOIN accounts_user u ON u.id = i.id_user \
       WHERE i.method = 'orcid') \
  TO 'orcid-users.csv' WITH (FORMAT csv, HEADER true)
```

Then run the script. It is a dry run unless `--apply` is given, and re-running is safe —
rows already linked are counted and skipped:

```bash
export KEYCLOAK_URL=https://auth.front-matter.de
export KEYCLOAK_REALM=main

# either an admin user …
export KEYCLOAK_ADMIN_USER=admin
read -rs KEYCLOAK_ADMIN_PASSWORD && export KEYCLOAK_ADMIN_PASSWORD

# … or a client's service account, scoped to this realm only
export KEYCLOAK_ADMIN_REALM=main
export KEYCLOAK_CLIENT_ID=rogue-scholar-backfill
read -rs KEYCLOAK_CLIENT_SECRET && export KEYCLOAK_CLIENT_SECRET

uv run --with structlog python scripts/link_orcid_identities.py orcid-users.csv
uv run --with structlog python scripts/link_orcid_identities.py orcid-users.csv --apply
```

Use `read -rs` rather than assigning the password inline: shells expand `!`, `$` and
(in zsh) `^`, which silently corrupts the value.

A service account needs two things, or the Admin API answers a bare `401`:

- the `manage-users` role from `realm-management`
- the **`kc-admin-api` client scope** as a default scope — required of custom clients
  since Keycloak 26.8.2

The report counts four outcomes. `conflicts` should always be zero; each one means an
ORCID iD is linked to a different account than its email points at, and wants a human.

## Before creating accounts

New accounts are created with an email but no names. If `firstName` and `lastName` are
required in **Realm settings → User profile**, every created account still meets the
`VERIFY_PROFILE` form at first login, which defeats the purpose of pre-creating them.
Relax those two first.

Note also that ORCID releases no email claim — only `sub`, `name`, `given_name` and
`family_name`, with `openid` as its sole scope. Accounts created by ORCID sign-in
therefore have no email and cannot use magic-link login until they supply one.
