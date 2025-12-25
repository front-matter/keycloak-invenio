# Build stage - compile extensions
FROM maven:3.9-eclipse-temurin-21 AS builder

# Install git for cloning repositories
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Build auto-username mapper
COPY auto-username/ ./auto-username/
RUN cd auto-username && mvn clean package -DskipTests

# Clone and build keycloak-2fa-email-authenticator
RUN git clone https://github.com/mesutpiskin/keycloak-2fa-email-authenticator.git && \
  cd keycloak-2fa-email-authenticator && \
  mvn clean package && \
  ls -la target/*.jar

# Final stage
FROM quay.io/keycloak/keycloak:26.4

# Install keycloak-orcid extension
ADD --chmod=644 https://github.com/eosc-kc/keycloak-orcid/releases/download/1.4.0/keycloak-orcid.jar /opt/keycloak/providers/keycloak-orcid.jar

# Copy custom auto-username mapper
COPY --from=builder /build/auto-username/target/auto-username.jar /opt/keycloak/providers/

# Copy Email OTP authenticator
COPY --from=builder /build/keycloak-2fa-email-authenticator/target/keycloak-2fa-email-authenticator-*.jar /opt/keycloak/providers/

# Copy Keycloakify theme JAR
COPY dist_keycloak/keycloak-theme-front-matter.jar /opt/keycloak/providers/

# Verify all providers are in place
RUN ls -la /opt/keycloak/providers/ && \
  echo "Verifying extensions..." && \
  ls -1 /opt/keycloak/providers/*.jar

# Build Keycloak with all providers (required for production)
RUN /opt/keycloak/bin/kc.sh build

# Test that extensions are properly loaded
RUN /opt/keycloak/bin/kc.sh show-config 2>&1 | grep -q "spi-" && echo "✓ SPI configuration loaded" || (echo "✗ SPI configuration not found" && exit 1)

# Verify Email OTP provider is available by checking JAR contents
RUN jar -tf /opt/keycloak/providers/keycloak-2fa-email-authenticator-*.jar | grep -q "EmailAuthenticatorFormFactory" && \
  echo "✓ Email OTP authenticator classes found" || \
  (echo "✗ Email OTP authenticator classes not found" && exit 1)

# Verify ORCID provider is available
RUN jar -tf /opt/keycloak/providers/keycloak-orcid.jar | grep -q "OrcidIdentityProviderFactory" && \
  echo "✓ ORCID provider classes found" || \
  (echo "✗ ORCID provider classes not found" && exit 1)

# Verify auto-username mapper is available
RUN jar -tf /opt/keycloak/providers/auto-username.jar | grep -q "AutoUsernameMapperFactory" && \
  echo "✓ Auto-username mapper classes found" || \
  (echo "✗ Auto-username mapper classes not found" && exit 1)

# Verify SPI registration files exist
RUN jar -tf /opt/keycloak/providers/keycloak-2fa-email-authenticator-*.jar | grep -q "META-INF/services/org.keycloak.authentication.AuthenticatorFactory" && \
  echo "✓ Email OTP SPI registration found" || \
  (echo "✗ Email OTP SPI registration not found" && exit 1)

RUN echo "============================================" && \
  echo "All extensions verified successfully!" && \
  echo "- ORCID Identity Provider" && \
  echo "- Email OTP Authenticator" && \
  echo "- Auto Username Mapper" && \
  echo "- Keycloakify Theme" && \
  echo "============================================"
