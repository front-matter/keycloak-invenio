package org.invenio.keycloak.magiclink;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for Magic Link Authenticator
 */
public class MagicLinkAuthenticatorFactory implements AuthenticatorFactory {

  public static final String PROVIDER_ID = "magic-link-authenticator";

  @Override
  public String getDisplayType() {
    return "Magic Link";
  }

  @Override
  public String getReferenceCategory() {
    return "passwordless";
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return new AuthenticationExecutionModel.Requirement[] {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };
  }

  @Override
  public boolean isUserSetupAllowed() {
    return false;
  }

  @Override
  public String getHelpText() {
    return "Passwordless authentication using email magic links. Users enter their email and receive a one-time login link.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    List<ProviderConfigProperty> properties = new ArrayList<>();

    ProviderConfigProperty createUser = new ProviderConfigProperty();
    createUser.setType(ProviderConfigProperty.BOOLEAN_TYPE);
    createUser.setName("createUser");
    createUser.setLabel("Auto-create users");
    createUser.setHelpText("Automatically create user accounts for new email addresses");
    createUser.setDefaultValue(false);
    properties.add(createUser);

    ProviderConfigProperty tokenValidity = new ProviderConfigProperty();
    tokenValidity.setType(ProviderConfigProperty.STRING_TYPE);
    tokenValidity.setName("tokenValidity");
    tokenValidity.setLabel("Token validity (seconds)");
    tokenValidity.setHelpText("How long the magic link remains valid. Default: 3600 (1 hour)");
    tokenValidity.setDefaultValue("3600");
    properties.add(tokenValidity);

    return properties;
  }

  @Override
  public Authenticator create(KeycloakSession session) {
    return new MagicLinkAuthenticator();
  }

  @Override
  public void init(Config.Scope config) {
    // No initialization needed
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    // No post-initialization needed
  }

  @Override
  public void close() {
    // Nothing to close
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
