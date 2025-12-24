FROM quay.io/keycloak/keycloak:26.4

# Install keycloak-orcid extension
ADD --chmod=644 https://github.com/eosc-kc/keycloak-orcid/releases/download/1.4.0/keycloak-orcid.jar /opt/keycloak/providers/keycloak-orcid.jar

# Copy Keycloakify theme JAR
COPY dist_keycloak/keycloak-theme-front-matter.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
