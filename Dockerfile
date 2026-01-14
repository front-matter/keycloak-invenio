# Build stage - compile extensions
FROM maven:3.9-eclipse-temurin-21 AS builder

# Install tools for fetching/building sources
RUN apt-get update && apt-get install -y git curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Build auto-username mapper
COPY auto-username/ ./auto-username/
RUN cd auto-username && mvn clean package -DskipTests

# Build magic-link authenticator
COPY magic-link/ ./magic-link/
RUN cd magic-link && mvn clean package -DskipTests

# Clone and build keycloak-orcid with Keycloak 26.4 compatibility patch
RUN git clone --depth 1 --branch 1.4.0 https://github.com/eosc-kc/keycloak-orcid.git && \
  cd keycloak-orcid && \
  sed -i 's/user\.setIdp(this);/\/\/ user.setIdp(this); \/\/ Removed for Keycloak 26.4+ compatibility/g' src/main/java/org/keycloak/social/orcid/OrcidIdentityProvider.java && \
  mvn clean package -DskipTests

# Final stage
FROM quay.io/keycloak/keycloak:26.4

# Copy keycloak-orcid from builder
COPY --from=builder /build/keycloak-orcid/target/keycloak-orcid.jar /opt/keycloak/providers/

# Copy custom auto-username mapper
COPY --from=builder /build/auto-username/target/auto-username.jar /opt/keycloak/providers/

# Copy magic-link authenticator
COPY --from=builder /build/magic-link/target/magic-link.jar /opt/keycloak/providers/

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
  test -f /opt/keycloak/providers/auto-username.jar && echo "✓ Auto Username Mapper" || (echo "✗ Auto-username missing" && exit 1) && \
  test -f /opt/keycloak/providers/magic-link.jar && echo "✓ Magic Link Authenticator" || (echo "✗ Magic Link missing" && exit 1) && \
  test -f /opt/keycloak/providers/keycloak-theme-for-kc-all-other-versions.jar && echo "✓ Keycloakify Theme" || (echo "✗ Theme missing" && exit 1) && \
  echo "============================================" && \
  echo "All extensions installed successfully!"
