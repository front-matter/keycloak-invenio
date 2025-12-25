package org.frontmatter.keycloak.username;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

/**
 * Identity Provider Mapper that automatically generates usernames.
 * Compatible with all identity providers.
 */
public class AutoUsernameMapperFactory extends AbstractIdentityProviderMapper {

  public static final String PROVIDER_ID = "auto-username-mapper";
  public static final String[] COMPATIBLE_PROVIDERS = { ANY_PROVIDER };
  private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(
      Arrays.asList(IdentityProviderSyncMode.values()));

  @Override
  public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
    return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
  }

  @Override
  public String[] getCompatibleProviders() {
    return COMPATIBLE_PROVIDERS;
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
    return "Automatically generates a random username using Crockford Base32 encoding (format: usr_xxxxxxxx). Compatible with all identity providers.";
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

    throw new RuntimeException("Failed to generate unique username after 3 retries");
  }
}
