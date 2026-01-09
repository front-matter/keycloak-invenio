# Keycloak with ORCID Integration and Email OTP

A custom Keycloak Docker image with the [ORCID Identity Provider extension](https://github.com/eosc-kc/keycloak-orcid) and Email OTP (One-Time Password) authenticator pre-installed, designed for use with InvenioRDM and other research data management platforms.

## Features

- Based on Keycloak 26.4
- Pre-installed ORCID Social Identity Provider extension
- ORCID-specific user attribute mappers
- ORCID theme with logo support
- Email OTP two-factor authentication
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

## Email OTP Two-Factor Authentication

This image includes the [keycloak-2fa-email-authenticator](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator) extension for email-based one-time password authentication.

### Prerequisites

The Email OTP extension is built from source during the Docker image build process and is already included in the image.

### SMTP Configuration

Before Email OTP works, you need to configure your realm's SMTP settings:

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
3. Enter a name (e.g., "Browser with Email OTP")
4. **Top level flow type**: `basic-flow`
5. Click **Create**

#### Configure the Flow

1. Click **Add execution** for the new flow
2. Select **Cookie** → **Add**
3. Set **Cookie** to `ALTERNATIVE`

4. Click **Add execution** again
5. Select **Identity Provider Redirector** → **Add**
6. Set to `ALTERNATIVE`

7. Click **Add sub-flow**
8. Name: "Forms" or similar
9. Set to `ALTERNATIVE`

10. For the "Forms" sub-flow:
    - **Add execution** → **Username Password Form** → `REQUIRED`
    - **Add execution** → **Email OTP** → `REQUIRED`

#### Activate the Flow

1. Go to **Authentication** → **Bindings**
2. Select your new flow for **Browser Flow**
3. Click **Save**

### Email OTP Configuration

Click the ⚙️ (Settings) icon next to **Email OTP** in the authentication flow:

#### Available Options

- **Code length**: Length of the OTP code (default: 6 digits)
- **Time-to-live**: Validity of the code in seconds (default: 300 = 5 minutes)
- **Simulation mode**: For development - emails won't be sent but printed to server logs

### Conditional Email OTP (Optional)

If you need more control, you can use **Conditional Email OTP**:

#### Additional Configuration Options

- **OTP control User Attribute**: User attribute to control OTP (values: `force`, `skip`)
- **Skip OTP for Role**: OTP is skipped if user has this role
- **Force OTP for Role**: OTP is always required if user has this role
- **Skip OTP for Header**: Skip OTP based on HTTP header pattern (e.g., trusted IP addresses)
- **Force OTP for Header**: Force OTP based on HTTP header pattern
- **Fallback OTP handling**: Default behavior (`skip` or `force`)

#### Example: OTP Only for External Access

```
Skip OTP for Header: X-Forwarded-Host: (192.168.1.*|10.0.0.*)
Fallback OTP handling: force
```

### Testing

1. Create a test user with a valid email address
2. Log in
3. After successful password entry, you should receive an OTP code via email
4. Enter the code on the next page
5. You have 5 minutes (or your configured TTL value)

### Troubleshooting

#### Emails Not Being Sent

- Verify SMTP configuration in **Realm settings** → **Email**
- Test the connection with the **Test connection** button
- Temporarily enable **Simulation mode** and check Keycloak logs

#### Enable Required Action

The Email OTP authenticator sets a Required Action (`email-authenticator-setup`) for users who haven't been configured yet.

If this Required Action is disabled, login may fail with **"Credential setup required"**.

**Authentication → Required actions:**
1. Find **"Set up Email Authenticator"**
2. Set **Enabled** = **ON**

#### Code Invalid or Expired

- Check the **Time-to-live** setting
- Ensure server and client system times are synchronized
- Use the **Resend code** button if available

#### User Has No Email Address

Email OTP requires a valid email address in the user profile. Make sure:
- Email address is entered in the user profile
- Email address is verified (optional but recommended)

### Security Recommendations

1. Use sufficient **Code length** (minimum 6 digits)
2. Set appropriate **Time-to-live** (300-600 seconds)
3. Enable **SMTP Authentication** and use TLS/SSL
4. Combine Email OTP with other factors for enhanced security
5. Monitor failed OTP attempts in Keycloak logs
6. Use **Conditional Email OTP** to differentiate based on roles or network access

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

## Email OTP Extension Details

- **Source**: [mesutpiskin/keycloak-2fa-email-authenticator](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator)
- **Built from**: Main branch (built during Docker image creation)
- **Compatibility**: Keycloak 26.x

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
