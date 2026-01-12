# Build stage - compile extensions
FROM maven:3.9-eclipse-temurin-21 AS builder

# Install tools for fetching/building sources
RUN apt-get update && apt-get install -y git curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Build auto-username mapper
COPY auto-username/ ./auto-username/
RUN cd auto-username && mvn clean package -DskipTests

# Clone and build keycloak-2fa-email-authenticator from magic-link branch
RUN git clone --depth 1 --branch magic-link https://github.com/front-matter/keycloak-2fa-email-authenticator.git && \
  cd keycloak-2fa-email-authenticator && \
  mvn clean package -DskipTests

# Clone and build keycloak-orcid with Keycloak 26.5 compatibility patch
RUN git clone --depth 1 --branch 1.4.0 https://github.com/eosc-kc/keycloak-orcid.git && \
  cd keycloak-orcid && \
  sed -i 's/user\.setIdp(this);/\/\/ user.setIdp(this); \/\/ Removed for Keycloak 26.5+ compatibility/g' src/main/java/org/keycloak/social/orcid/OrcidIdentityProvider.java && \
  mvn clean package -DskipTests

# Final stage
FROM quay.io/keycloak/keycloak:26.5

# Copy keycloak-orcid from builder
COPY --from=builder /build/keycloak-orcid/target/keycloak-orcid.jar /opt/keycloak/providers/

# Copy custom auto-username mapper
COPY --from=builder /build/auto-username/target/auto-username.jar /opt/keycloak/providers/

# Copy Email OTP authenticator
COPY --from=builder /build/keycloak-2fa-email-authenticator/target/keycloak-2fa-email-authenticator-*.jar /opt/keycloak/providers/

# Copy Keycloakify theme JAR
COPY ./keycloakify/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar /opt/keycloak/providers/

# Copy email templates for Email OTP authenticator
COPY ./keycloakify/src/login/resources/email /opt/keycloak/themes/invenio/email

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
  ls /opt/keycloak/providers/keycloak-2fa-email-authenticator-*.jar >/dev/null 2>&1 && echo "✓ Email OTP Authenticator" || (echo "✗ Email OTP missing" && exit 1) && \
  test -f /opt/keycloak/providers/auto-username.jar && echo "✓ Auto Username Mapper" || (echo "✗ Auto-username missing" && exit 1) && \
  test -f /opt/keycloak/providers/keycloak-theme-for-kc-all-other-versions.jar && echo "✓ Keycloakify Theme" || (echo "✗ Theme missing" && exit 1) && \
  echo "============================================" && \
  echo "All extensions installed successfully!"
