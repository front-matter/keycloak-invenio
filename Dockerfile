# Build stage - use Maven image to compile the extension
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
RUN curl -sL https://github.com/eosc-kc/keycloak-orcid/archive/refs/tags/1.4.0.tar.gz | tar xz && \
  cd keycloak-orcid-1.4.0 && \
  mvn clean package -DskipTests

# Final stage
FROM quay.io/keycloak/keycloak:26.4

# Copy built extension from builder
COPY --from=builder /build/keycloak-orcid-1.4.0/target/keycloak-orcid-*.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
