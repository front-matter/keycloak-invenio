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
  mvn clean package -DskipTests

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

RUN /opt/keycloak/bin/kc.sh build
