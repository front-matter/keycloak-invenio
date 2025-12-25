#!/bin/bash
# Script to verify auto-username mapper installation

echo "=== Checking Docker image ==="
docker run --rm --entrypoint sh ghcr.io/front-matter/keycloak-invenio:latest -c "
  echo 'Providers directory:'
  ls -lh /opt/keycloak/providers/auto-username.jar
  
  echo ''
  echo 'JAR contents:'
  unzip -l /opt/keycloak/providers/auto-username.jar | grep -E 'AutoUsername|META-INF/services'
  
  echo ''
  echo 'SPI registration:'
  unzip -p /opt/keycloak/providers/auto-username.jar META-INF/services/org.keycloak.broker.provider.IdentityProviderMapper
"

echo ""
echo "=== To redeploy ==="
echo "1. Stop your Keycloak container"
echo "2. Remove the container: docker rm <container-name>"
echo "3. Start fresh with: docker run -d -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin ghcr.io/front-matter/keycloak-invenio:latest start-dev"
echo "4. Wait for startup (check logs: docker logs -f <container-name>)"
echo "5. Access http://localhost:8080 and check Admin UI"
