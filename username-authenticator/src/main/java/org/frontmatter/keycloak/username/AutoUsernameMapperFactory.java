package org.frontmatter.keycloak.username;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class AutoUsernameMapperFactory extends AbstractIdentityProviderMapper {

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
    return "Automatically generates a random username for new users imported from identity providers. Uses Crockford Base32 encoding for readability.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return new ArrayList<>();
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
