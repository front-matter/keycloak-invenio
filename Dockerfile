# Build stage - compile extensions
FROM maven:3.9-eclipse-temurin-21 AS builder

# Install git for cloning repositories
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Build auto-username mapper
COPY auto-username/ ./auto-username/
RUN cd auto-username && mvn clean package -DskipTests

# Clone and build keycloak-2fa-email-authenticator
# Note: Tests are skipped as they exist only in local development
RUN git clone https://github.com/mesutpiskin/keycloak-2fa-email-authenticator.git && \
  cd keycloak-2fa-email-authenticator && \
  mvn clean package -DskipTests || (echo "Maven build failed" && exit 1) && \
  echo "Built JAR files:" && \
  ls -la target/ && \
  if [ ! -f target/keycloak-2fa-email-authenticator*.jar ]; then \
  echo "ERROR: JAR file not found in target directory"; \
  exit 1; \
  fi

# Final stage
FROM quay.io/keycloak/keycloak:26.4

# Install keycloak-orcid extension
ADD --chmod=644 https://github.com/eosc-kc/keycloak-orcid/releases/download/1.4.0/keycloak-orcid.jar /opt/keycloak/providers/keycloak-orcid.jar

# Copy custom auto-username mapper
COPY --from=builder /build/auto-username/target/auto-username.jar /opt/keycloak/providers/

# Copy Email OTP authenticator (wildcard to handle version-specific names)
COPY --from=builder /build/keycloak-2fa-email-authenticator/target/keycloak-2fa-email-authenticator*.jar /opt/keycloak/providers/keycloak-2fa-email-authenticator.jar

# Copy Keycloakify theme JAR
COPY ./keycloakify/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar /opt/keycloak/providers/

# Copy realm configuration
COPY realm-config.json /opt/keycloak/data/import/realm-config.json

# Verify all providers are in place
RUN ls -la /opt/keycloak/providers/ && \
  echo "Verifying extensions..." && \
  ls -1 /opt/keycloak/providers/*.jar

# Build Keycloak with all providers (required for production)
RUN /opt/keycloak/bin/kc.sh build

# Verify all extension JARs are present
RUN echo "============================================" && \
  echo "Verifying installed extensions:" && \
  ls -1 /opt/keycloak/providers/ && \
  test -f /opt/keycloak/providers/keycloak-orcid.jar && echo "✓ ORCID Identity Provider" || (echo "✗ ORCID provider missing" && exit 1) && \
  test -f /opt/keycloak/providers/keycloak-2fa-email-authenticator.jar && echo "✓ Email OTP Authenticator" || (echo "✗ Email OTP missing" && exit 1) && \
  test -f /opt/keycloak/providers/auto-username.jar && echo "✓ Auto Username Mapper" || (echo "✗ Auto-username missing" && exit 1) && \
  test -f /opt/keycloak/providers/keycloak-theme-for-kc-all-other-versions.jar && echo "✓ Keycloakify Theme" || (echo "✗ Theme missing" && exit 1) && \
  echo "============================================" && \
  echo "All extensions installed successfully!"
