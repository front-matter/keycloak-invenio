# Keycloak Magic Link Authenticator

Passwordless authentication extension for Keycloak that allows users to log in by clicking a one-time link sent to their email address.

## Features

- 🔐 **Passwordless authentication** - Users receive a secure login link via email
- ⚡ **One-time use tokens** - Links expire after use or timeout
- 🔧 **Configurable** - Token validity and auto-user-creation settings
- ✉️ **Email verification** - Automatically marks email as verified
- 🎨 **Customizable templates** - FreeMarker templates for email and confirmation page
- 🧪 **Fully tested** - Comprehensive unit test coverage

## Requirements

- Keycloak 26.5 or higher
- Java 21
- Maven 3.9+
- Configured email provider (SMTP)

## Installation

### 1. Build the Extension

```bash
cd magic-link
mvn clean package -DskipTests
```

This produces `target/magic-link.jar` (~16KB)

### 2. Deploy to Keycloak

#### Option A: Docker Build (Recommended)
```bash
# Already integrated in Dockerfile
docker build -t keycloak-invenio .
```

#### Option B: Manual Deployment
```bash
# Copy JAR to Keycloak providers directory
cp target/magic-link.jar /opt/keycloak/providers/

# Rebuild Keycloak
/opt/keycloak/bin/kc.sh build
```

### 3. Restart Keycloak
```bash
docker-compose restart keycloak
# or
/opt/keycloak/bin/kc.sh start
```

## Configuration

### 1. Configure Email Provider

Navigate to: **Realm Settings → Email**

Configure SMTP settings:
- Host, Port, From address
- Authentication credentials
- SSL/TLS settings

Test the configuration before proceeding.

### 2. Create Authentication Flow

Navigate to: **Authentication → Flows**

1. **Create new flow:**
   - Name: `Magic Link Browser`
   - Type: `browser`

2. **Add execution:**
   - Click "Add execution"
   - Select "Magic Link"
   - Set requirement to `REQUIRED` or `ALTERNATIVE`

3. **Configure execution:**
   Click ⚙️ icon to configure:
   
   | Setting | Description | Default |
   |---------|-------------|---------|
   | **Auto-create users** | Create user accounts for new emails | `false` |
   | **Token validity** | Link expiration time in seconds | `3600` (1 hour) |

4. **Bind flow:**
   - Go to **Authentication → Bindings**
   - Set `Magic Link Browser` as **Browser Flow**

## Usage

### User Flow

1. User navigates to login page
2. Enters email address (no password required)
3. Receives email with login link
4. Clicks link in email
5. Automatically logged in and redirected

### API Flow

The magic link URL format:
```
https://{keycloak-host}/realms/{realm}/login-actions/action-token
  ?key={encrypted-token}
  &client_id={client-id}
```

Token contains:
- User ID
- Client ID
- Redirect URI
- Expiration timestamp
- Remember-me flag

## Email Templates

Templates are located in `src/main/resources/theme-resources/`

### Customize Email Template

Edit `templates/magic-link.ftl`:
```html
<#import "template.ftl" as layout>
<@layout.emailLayout>
    <p>${kcSanitize(msg("magicLinkEmailBody"))?no_esc}</p>
    <p>
        <a href="${link}">${msg("magicLinkButton")}</a>
    </p>
    <p>${msg("magicLinkExpiration", linkExpiration)}</p>
</@layout.emailLayout>
```

Variables available:
- `${link}` - The magic login link
- `${linkExpiration}` - Expiration time in minutes
- `${user.email}` - User's email address
- `${realm.displayName}` - Realm name

### Customize Confirmation Page

Edit `templates/magic-link-sent.ftl` for the "Check your email" page.

### Translations

Add messages to `messages/messages_en.properties`:
```properties
magicLinkSubject=Your login link
magicLinkEmailBody=Click the button to log in. Valid for {0} minutes.
magicLinkButton=Log In
```

For additional languages, create `messages_de.properties`, `messages_fr.properties`, etc.

## Development

### Build and Test

```bash
# Run tests
mvn test

# Run specific test
mvn test -Dtest=MagicLinkAuthenticatorTest

# Build without tests
mvn clean package -DskipTests

# Build with tests
mvn clean package
```

### Project Structure

```
magic-link/
├── src/
│   ├── main/
│   │   ├── java/org/invenio/keycloak/magiclink/
│   │   │   ├── MagicLinkActionToken.java          # JWT token model
│   │   │   ├── MagicLinkActionTokenHandler.java   # Processes token
│   │   │   ├── MagicLinkAuthenticator.java        # Shows form & sends email
│   │   │   ├── MagicLinkAuthenticatorFactory.java # SPI registration
│   │   │   └── MagicLinkActionTokenHandlerFactory.java
│   │   └── resources/
│   │       ├── META-INF/services/                 # SPI providers
│   │       └── theme-resources/
│   │           ├── messages/                      # i18n strings
│   │           └── templates/                     # FreeMarker templates
│   └── test/java/                                 # Unit tests (23 tests)
├── pom.xml
└── README.md
```

### Key Components

1. **MagicLinkAuthenticator** - Shows email form, generates token, sends email
2. **MagicLinkActionToken** - JWT containing user info and expiration
3. **MagicLinkActionTokenHandler** - Validates token, logs user in
4. **Factories** - Register components with Keycloak SPI

## Security Considerations

- ✅ Tokens are single-use (Keycloak handles revocation)
- ✅ Configurable expiration time
- ✅ Redirect URI validation prevents phishing
- ✅ Email verification automatic
- ✅ Rate limiting depends on email provider
- ✅ No password stored or transmitted

### Best Practices

1. **Use short expiration times** (5-15 minutes) for high-security scenarios
2. **Configure rate limiting** at email provider level
3. **Monitor failed login attempts** via Keycloak events
4. **Use HTTPS** for all Keycloak endpoints
5. **Enable DMARC/SPF/DKIM** for email authentication

## Troubleshooting

### Email not sent

**Check:**
1. SMTP configuration in Realm Settings → Email
2. Keycloak logs: `/opt/keycloak/data/log/`
3. Email provider rate limits
4. Spam folder

**Test email:**
```bash
# Via Keycloak Admin Console
Realm Settings → Email → Test connection
```

### Link expired

**Solutions:**
- Increase `tokenValidity` in authenticator config
- User can request new link (click "Request New Link")

### Link not working

**Check:**
1. Token parameter in URL: `?key=...`
2. Client ID matches requesting client
3. Redirect URI is whitelisted in client settings
4. No URL encoding issues

**Debug:**
Enable debug logging in Keycloak:
```bash
# In standalone.xml or kc.sh
--log-level=DEBUG
```

### User not created

**If auto-create disabled:**
- User must exist in Keycloak before magic link login
- Create users manually or enable `Auto-create users` option

**If auto-create enabled:**
- Check Keycloak events for registration errors
- Verify user creation permissions

### Tests failing

**Java 25 compatibility:**
```bash
# Set experimental flag for Byte Buddy
mvn test -Dnet.bytebuddy.experimental=true
```

Already configured in `pom.xml`:
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=true</argLine>
    </configuration>
</plugin>
```

## Advanced Configuration

### Custom Token Validity per Client

Implement custom logic in `MagicLinkAuthenticator.getTokenValidity()`:

```java
private int getTokenValidity(AuthenticationFlowContext context) {
    String clientId = context.getSession().getContext().getClient().getClientId();
    
    // Custom logic per client
    if ("high-security-app".equals(clientId)) {
        return 300; // 5 minutes
    }
    
    // Default from config
    return getConfiguredValidity(context);
}
```

### Integration with External User Stores

Override `MagicLinkAuthenticator.createUser()` to sync with external systems:

```java
private UserModel createUser(AuthenticationFlowContext context, String email) {
    // Create in Keycloak
    UserModel user = context.getSession().users().addUser(context.getRealm(), email);
    
    // Sync to external system
    externalUserService.createUser(email);
    
    return user;
}
```

### Custom Email Sender

Implement custom `EmailTemplateProvider` for advanced scenarios:
- Custom email service (SendGrid, Amazon SES)
- Email templates from database
- Multi-channel delivery (SMS + Email)

## API Reference

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `createUser` | boolean | `false` | Auto-create user accounts |
| `tokenValidity` | integer | `3600` | Token lifetime in seconds |

### Events

Magic link operations emit Keycloak events:

| Event | Description |
|-------|-------------|
| `EXECUTE_ACTION_TOKEN` | Token clicked and validated |
| `LOGIN` | User logged in via magic link |
| `REGISTER` | New user created (if auto-create enabled) |
| `EMAIL_SEND_FAILED` | Email delivery failed |

Query events: **Events → Login Events / Admin Events**

## Performance

- **Token generation:** ~1ms
- **Email sending:** Depends on SMTP provider
- **Token validation:** ~2ms
- **Memory footprint:** ~50KB per authenticator instance

Scales linearly with user count and email throughput.

## License

Same as parent Keycloak InvenioRDM project.

## Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review Keycloak logs
3. Open issue in repository

## Version History

- **1.0.0** (2026-01-12) - Initial release
  - Passwordless authentication via email
  - Configurable token validity
  - Auto-user-creation option
  - Email templates and i18n support
  - Comprehensive test suite
