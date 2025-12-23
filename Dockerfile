FROM quay.io/keycloak/keycloak:26.4

# Install keycloak-orcid extension
ADD https://github.com/eosc-kc/keycloak-orcid/releases/download/1.4.0/keycloak-orcid-1.4.0.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
