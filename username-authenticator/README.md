# Username Authenticator for Keycloak

Automatically generates random, secure usernames for new Keycloak users.

## Features

- Generates random 8-character usernames with `usr_` prefix
- Uses Crockford Base32 encoding for readability (excludes ambiguous characters like `i`, `l`, `o`, `u`)
- Collision detection with retry logic
- Implements Keycloak Authenticator SPI

## Username Format

Generated usernames follow the pattern: `usr_XXXXXXXX`

Example: `usr_3k8d9ma2`

The username generator uses Crockford Base32 encoding, which is also used by InvenioRDM for record recids. This ensures consistency across the system and provides human-readable, unambiguous identifiers.

## Building

Build the JAR file using Maven:

```bash
cd username-authenticator
mvn clean package
```

The compiled JAR will be in `target/username-authenticator.jar`

### Running Tests

```bash
mvn test
```

**Note**: The test suite currently includes comprehensive tests for the username generation logic (109 tests). Integration tests for the Keycloak Authenticator components are skipped due to Java 25 compatibility issues with Mockito and Keycloak interfaces. The core functionality (username generation) is thoroughly tested.

## Installation

### With Docker (Recommended)

The authenticator is automatically built and included when building the main Dockerfile.

### Manual Installation

1. Copy the JAR file to Keycloak's providers directory:
   ```bash
   cp target/username-authenticator.jar /opt/keycloak/providers/
   ```

2. Rebuild Keycloak:
   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

3. Restart Keycloak

## Configuration in Keycloak Admin Console

### Option 1: Identity Provider Mapper (Recommended)

This is the recommended approach as it assigns usernames during user import, not after.

1. Go to **Identity Providers** → Select your provider (e.g., **ORCID**)
2. Click on the **Mappers** tab
3. Click **Add mapper**
4. Select **Auto Username Generator** from the mapper type dropdown
5. Give it a name (e.g., "Auto Username")
6. Save

Now when users log in via ORCID for the first time, they'll automatically get a unique username assigned during the import process.

### Option 2: Authentication Flow (Alternative)

For users logging in via ORCID or other identity providers:

1. Go to **Identity Providers** → Select your provider (e.g., ORCID)
2. Under **First Login Flow**, select the flow containing the Auto Username Generator
3. Save

Now when users log in for the first time via ORCID, they'll automatically get a unique username assigned.

## Technical Details

- **Package**: `org.frontmatter.keycloak.username`
- **Provider ID**: `auto-username-authenticator`
- **SPI Type**: Authenticator
- **Java Version**: 21
- **Keycloak Version**: 26.0.0+

## Code Structure

- `UsernameGenerator.java` - Generates random usernames using SecureRandom
- `AutoUsernameAuthenticator.java` - Main authenticator logic
- `AutoUsernameAuthenticatorFactory.java` - SPI factory for registration

## License

MIT License
