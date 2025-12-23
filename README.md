# Keycloak with ORCID Integration

A custom Keycloak Docker image with the [ORCID Identity Provider extension](https://github.com/eosc-kc/keycloak-orcid) pre-installed, designed for use with InvenioRDM and other research data management platforms.

## Features

- Based on Keycloak 26.4
- Pre-installed ORCID Social Identity Provider extension
- ORCID-specific user attribute mappers
- ORCID theme with logo support
- Automated builds via GitHub Actions
- Published to GitHub Container Registry

## Quick Start

Pull and run the pre-built image:

```bash
docker pull ghcr.io/front-matter/keycloak-invenio:latest
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin ghcr.io/front-matter/keycloak-invenio:latest start-dev
```

Access Keycloak at http://localhost:8080

## Configuration

### Setting up ORCID Identity Provider

1. Log in to the Keycloak Admin Console
2. Select your realm
3. Navigate to **Identity Providers** → **Add provider** → **ORCID**
4. You'll need to register your application with ORCID:
   - Visit [ORCID Developer Tools](https://orcid.org/developer-tools)
   - Click "Register for the free ORCID public API"
   - Configure your application with the Redirect URI from Keycloak
   - Format: `https://<keycloak-url>/realms/<realm>/broker/orcid/endpoint`
5. Enter the Client ID and Client Secret from ORCID into Keycloak
6. Save the configuration

### Optional: ORCID Theme

To use the ORCID theme with the logo:

1. Go to **Realm Settings** → **Themes**
2. Set Login Theme to `orcid-theme`
3. Save changes

## Building from Source

Build the Docker image locally:

```bash
git clone https://github.com/front-matter/keycloak-invenio.git
cd keycloak-invenio
docker build -t keycloak-invenio:local .
```

## GitHub Actions

The repository includes automated builds that:

- Build on every push to `main` branch (creates `latest` tag)
- Build on version tags (e.g., `v1.0.0` creates versioned tags)
- Publish to `ghcr.io/front-matter/keycloak-invenio`

To create a new release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Integration with InvenioRDM

This Keycloak image is designed to work seamlessly with InvenioRDM. Configure InvenioRDM to use this Keycloak instance as an OAuth provider with ORCID authentication.

## ORCID Extension Details

- **Version**: 1.4.0
- **Source**: [eosc-kc/keycloak-orcid](https://github.com/eosc-kc/keycloak-orcid)
- **Compatibility**: Keycloak 25.0.0+

## License

- Keycloak: Apache License 2.0
- ORCID Extension: See [extension license](https://github.com/eosc-kc/keycloak-orcid/blob/main/LICENCE)
- This repository: MIT License

## Support

For issues related to:
- This Docker image: Open an issue in this repository
- ORCID extension: Visit [eosc-kc/keycloak-orcid](https://github.com/eosc-kc/keycloak-orcid/issues)
- Keycloak itself: Visit [Keycloak documentation](https://www.keycloak.org/documentation)
