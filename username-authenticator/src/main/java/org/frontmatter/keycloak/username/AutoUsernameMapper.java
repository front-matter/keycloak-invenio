package org.frontmatter.keycloak.username;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class AutoUsernameMapper extends AbstractIdentityProviderMapper {

  public static final String PROVIDER_ID = "auto-username-mapper";

  @Override
  public String[] getCompatibleProviders() {
    // Works with all identity providers - return empty array to indicate
    // compatibility with all
    return new String[0];
  }

  @Override
  public String getDisplayCategory() {
    return "Username Importer";
  }

  @Override
  public String getDisplayType() {
    return "Auto Username Generator";
  }

  @Override
  public String getHelpText() {
    return "Automatically generates a random username for new users imported from identity providers.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return new ArrayList<>();
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
      IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
    assignUsername(session, realm, user);
  }

  @Override
  public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
      IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
    // Only assign username if missing
    if (user.getUsername() == null || user.getUsername().isEmpty()) {
      assignUsername(session, realm, user);
    }
  }

  private void assignUsername(KeycloakSession session, RealmModel realm, UserModel user) {
    for (int attempt = 0; attempt < 3; attempt++) {
      String username = UsernameGenerator.generate();

      if (session.users().getUserByUsername(realm, username) == null) {
        user.setUsername(username);
        return;
      }
    }

    throw new RuntimeException("Failed to generate unique username after retries");
  }
}
