# Auto Username Mapper for Keycloak

Identity Provider Mapper that automatically generates random usernames for users logging in via external identity providers (ORCID, Google, GitHub, etc.).

## Features

- Generates random 8-character usernames with `usr_` prefix
- Uses Crockford Base32 encoding for readability (excludes ambiguous characters like `i`, `l`, `o`, `u`)
- Collision detection with retry logic (3 attempts)
- Works with all identity providers
- Implements Keycloak Identity Provider Mapper SPI

## Username Format

Generated usernames follow the pattern: `usr_XXXXXXXX`

Example: `usr_3k8d9ma2`

The username generator uses Crockford Base32 encoding, which is also used by InvenioRDM for record recids. This ensures consistency across the system and provides human-readable, unambiguous identifiers.

## Building

Build the JAR file using Maven:

```bash
cd auto-username
mvn clean package
```

The compiled JAR will be in `target/auto-username.jar`

### Running Tests

```bash
mvn test
```

The test suite includes 109 comprehensive tests for the username generation logic, validating format, character set, uniqueness, and randomness properties.

## Installation

### With Docker (Recommended)

The mapper is automatically built and included when building the main Dockerfile:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY auto-username/ ./auto-username/
RUN cd auto-username && mvn clean package -DskipTests

FROM quay.io/keycloak/keycloak:26.4
COPY --from=builder /build/auto-username/target/auto-username.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build
```

### Manual Installation

1. Copy the JAR file to Keycloak's providers directory:
   ```bash
   cp target/auto-username.jar /opt/keycloak/providers/
   ```

2. Rebuild Keycloak:
   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

3. Restart Keycloak

## Configuration in Keycloak Admin Console

The mapper must be configured for each identity provider where you want automatic username generation.

### Setup Steps

1. Go to your realm (e.g., **main**)
2. Navigate to **Identity Providers** → Select your provider (e.g., **ORCID**)
3. Click on the **Mappers** tab
4. Click **Add mapper**
5. Select **Auto Username Generator** from the mapper type dropdown
6. Give it a name (e.g., "Auto Username")
7. Save

### Important: Disable Review Profile

To prevent users from seeing a username input form:

1. Go to **Authentication** → **Flows**
2. Select **First Broker Login** flow
3. Find **Review Profile** step
4. Set to **DISABLED**
5. Save

Now when users log in via your identity provider for the first time, they'll automatically get a unique username assigned during the import process without any manual input required.

## Technical Details

- **Package**: `org.frontmatter.keycloak.username`
- **Provider ID**: `auto-username-mapper`
- **Mapper Type**: Identity Provider Mapper
- **Display Name**: Auto Username Generator
- **Category**: Username Importer
- **Compatible Providers**: All identity providers
- **Java Version**: 21
- **Keycloak Version**: 26.0.0+

## Code Structure

- `UsernameGenerator.java` - Generates random usernames using SecureRandom and Crockford Base32
- `AutoUsernameMapper.java` - Identity Provider Mapper implementation

## How It Works

1. User logs in via external identity provider (e.g., ORCID)
2. Keycloak starts the first broker login flow
3. During user import, the mapper's `importNewUser()` method is called
4. A random username is generated and assigned before the user is created
5. If the username already exists, up to 3 retry attempts are made
6. User is created in Keycloak with the generated username

This approach ensures usernames are assigned at the correct time in the authentication flow, before any user interaction or review screens.

## License

MIT License
