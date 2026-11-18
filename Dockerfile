# Keycloak version of the image. Release tags are v<KEYCLOAK_VERSION>.<revision>,
# e.g. v26.8.2.0; the workflow checks that a tag matches this value.
ARG KEYCLOAK_VERSION=26.8.2

# Build stage - compile extensions
FROM maven:3.9-eclipse-temurin-21 AS builder

# Install tools for fetching/building sources
RUN apt-get update && apt-get install -y git curl ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Build auto-username mapper
COPY auto-username/ ./auto-username/
RUN cd auto-username && mvn clean package -DskipTests

# Theme, with the release in its stylesheet URL (theme.properties), so
# browsers load the stylesheet of each release instead of a cached one.
ARG THEME_VERSION=dev
COPY theme/ ./theme/
RUN case "$THEME_VERSION" in *[!A-Za-z0-9._-]*) echo "invalid THEME_VERSION" >&2; exit 1 ;; esac && \
  sed -i "s/@THEME_VERSION@/$THEME_VERSION/" theme/invenio/login/theme.properties && \
  grep -q "invenio.css?v=$THEME_VERSION" theme/invenio/login/theme.properties

# Build magic-link authenticator
COPY magic-link/ ./magic-link/
RUN cd magic-link && mvn clean package -DskipTests

# Clone and build Cloudflare Turnstile provider. The tag is checked against its
# commit, since a tag can be moved; update both together.
ARG TURNSTILE_VERSION=0.1.0-alpha.116
ARG TURNSTILE_COMMIT=75202a357f91da40969804358b6d197cbb848bc3
RUN git clone --depth 1 --branch "$TURNSTILE_VERSION" https://github.com/zymlabs/keycloak-cloudflare-turnstile-provider.git && \
  cd keycloak-cloudflare-turnstile-provider && \
  if [ "$(git rev-parse HEAD)" != "$TURNSTILE_COMMIT" ]; then \
    echo "Turnstile $TURNSTILE_VERSION is not commit $TURNSTILE_COMMIT" >&2; exit 1; \
  fi && \
  mvn clean package -DskipTests

# Final stage
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}

# Copy Cloudflare Turnstile provider
COPY --from=builder /build/keycloak-cloudflare-turnstile-provider/target/zymlabs-cloudflare-turnstile-provider.jar /opt/keycloak/providers/

# Copy custom auto-username mapper
COPY --from=builder /build/auto-username/target/auto-username.jar /opt/keycloak/providers/

# Copy magic-link authenticator
COPY --from=builder /build/magic-link/target/magic-link.jar /opt/keycloak/providers/

# Copy the login and email theme "invenio"
COPY --from=builder /build/theme/invenio/ /opt/keycloak/themes/invenio/

# Copy realm configuration
COPY realm-config.json /opt/keycloak/data/import/realm-config.json

# Verify all providers are in place
RUN ls -la /opt/keycloak/providers/ && \
  echo "Verifying extensions..." && \
  ls -1 /opt/keycloak/providers/*.jar

# Build Keycloak with all providers (required for production)
# --metrics-enabled and --health-enabled are baked in so the optimised start
# does not trigger a "re-augmentation required" warning at runtime.
RUN /opt/keycloak/bin/kc.sh build \
  --health-enabled=true --metrics-enabled=true

# Verify all extension JARs are present
RUN echo "============================================" && \
  echo "Verifying installed extensions:" && \
  ls -1 /opt/keycloak/providers/ && \
  test -f /opt/keycloak/providers/auto-username.jar && echo "✓ Auto Username Mapper" || (echo "✗ Auto-username missing" && exit 1) && \
  test -f /opt/keycloak/providers/magic-link.jar && echo "✓ Magic Link Authenticator" || (echo "✗ Magic Link missing" && exit 1) && \
  test -f /opt/keycloak/providers/zymlabs-cloudflare-turnstile-provider.jar && echo "✓ Cloudflare Turnstile Provider" || (echo "✗ Cloudflare Turnstile missing" && exit 1) && \
  test -f /opt/keycloak/themes/invenio/login/theme.properties && echo "✓ Theme invenio" || (echo "✗ Theme missing" && exit 1) && \
  echo "============================================" && \
  echo "All extensions installed successfully!"
