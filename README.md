# Keycloak with ORCID Integration and Magic Link Authentication

A custom Keycloak Docker image with the [ORCID Identity Provider extension](https://github.com/eosc-kc/keycloak-orcid) and Magic Link passwordless authenticator pre-installed, designed for use with InvenioRDM and other research data management platforms.

## Features

- Based on Keycloak 26.4
- Pre-installed ORCID Social Identity Provider extension
- ORCID-specific user attribute mappers
- ORCID theme with logo support
- Magic Link passwordless authentication
- Automatic username generation for ORCID users
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

## Magic Link Passwordless Authentication

This image includes a custom Magic Link authenticator extension for passwordless email-based authentication. Users receive a login link via email that automatically authenticates them when clicked.

### Prerequisites

The Magic Link extension is built from source during the Docker image build process and is already included in the image.

### SMTP Configuration

Before Magic Link authentication works, you need to configure your realm's SMTP settings:

1. Log in to Keycloak as admin
2. Switch to your realm
3. Click **Realm settings** in the left menu
4. Select the **Email** tab
5. Enter your SMTP details:
   - **Host**: SMTP server address
   - **Port**: SMTP port (e.g., 587 for TLS)
   - **From**: Sender email address
   - **From display name**: Display name for sender
   - **Enable SSL/StartTLS**: Based on your SMTP server configuration
   - **Authentication**: Username and password for SMTP authentication

### Configure Authentication Flow

#### Create New Browser Login Flow

1. Go to **Authentication** → **Flows**
2. Click **Create flow**
3. Enter a name (e.g., "Browser with Magic Link")
4. **Top level flow type**: `basic-flow`
5. Click **Create**

#### Configure the Flow

1. Click **Add execution** for the new flow
2. Select **Cookie** → **Add**
3. Set **Cookie** to `ALTERNATIVE`

4. Click **Add execution** again
5. Select **Identity Provider Redirector** → **Add**
6. Set to `ALTERNATIVE`

7. Click **Add execution**
8. Select **Magic Link Authenticator** → **Add**
9. Set to `ALTERNATIVE`

#### Activate the Flow

1. Go to **Authentication** → **Bindings**
2. Select your new flow for **Browser Flow**
3. Click **Save**

### Magic Link Configuration

Click the ⚙️ (Settings) icon next to **Magic Link Authenticator** in the authentication flow:

#### Available Options

- **Link validity**: How long the magic link remains valid (default: 900 seconds = 15 minutes)
- **Create user if not exists**: Automatically create users who don't have an account (default: disabled)

### Testing

1. Go to your application's login page
2. Enter an email address
3. Check your email inbox for the magic link
4. Click the link in the email
5. You will be automatically logged in

### Troubleshooting

#### Emails Not Being Sent

- Verify SMTP configuration in **Realm settings** → **Email**
- Test the connection with the **Test connection** button
- Check Keycloak server logs for any email errors

#### Link Expired or Invalid

- Check the **Link validity** setting (default: 15 minutes)
- Ensure server and client system times are synchronized
- Request a new magic link

#### User Has No Email Address

Magic Link authentication requires a valid email address. Make sure:
- Email address is entered in the user profile
- Email address format is valid

### Security Recommendations

1. Set appropriate **Link validity** duration (15-30 minutes recommended)
2. Enable **SMTP Authentication** and use TLS/SSL
3. Consider combining Magic Link with other authentication methods for sensitive operations
4. Monitor authentication logs for suspicious activity
5. Ensure users understand not to share magic links

## Email OTP Two-Factor Authentication

This image also includes the [keycloak-2fa-email-authenticator](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator) extension for email-based one-time password authentication as a second factor.

### Email OTP Configuration

For Email OTP configuration details, see the [keycloak-2fa-email-authenticator documentation](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator).

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

## Magic Link Extension Details

- **Built from**: Source (included in this repository)
- **Compatibility**: Keycloak 26.4+
- **Features**: Passwordless authentication via email links

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak Server Development](https://www.keycloak.org/docs/latest/server_development/index.html)
- [Keycloak Authentication SPI](https://www.keycloak.org/docs/latest/server_development/#_auth_spi)

## License

- Keycloak: Apache License 2.0
- ORCID Extension: See [extension license](https://github.com/eosc-kc/keycloak-orcid/blob/main/LICENCE)
- Email OTP Extension: Apache License 2.0
- This repository: MIT License

## Support

For issues related to:
- This Docker image: Open an issue in this repository
- ORCID extension: Visit [eosc-kc/keycloak-orcid](https://github.com/eosc-kc/keycloak-orcid/issues)
- Email OTP extension: Visit [mesutpiskin/keycloak-2fa-email-authenticator](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/issues)
- Keycloak itself: Visit [Keycloak documentation](https://www.keycloak.org/documentation)
