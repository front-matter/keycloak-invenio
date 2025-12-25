# Build stage - compile username authenticator
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY username-authenticator/ ./username-authenticator/
RUN cd username-authenticator && mvn clean package -DskipTests

# Final stage
FROM quay.io/keycloak/keycloak:26.4

# Install keycloak-orcid extension
ADD --chmod=644 https://github.com/eosc-kc/keycloak-orcid/releases/download/1.4.0/keycloak-orcid.jar /opt/keycloak/providers/keycloak-orcid.jar

# Copy custom username authenticator
COPY --from=builder /build/username-authenticator/target/username-authenticator.jar /opt/keycloak/providers/

# Copy Keycloakify theme JAR
COPY dist_keycloak/keycloak-theme-front-matter.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
