package org.invenio.keycloak.magiclink;

import org.keycloak.Config;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for Magic Link Action Token Handler
 */
public class MagicLinkActionTokenHandlerFactory implements ActionTokenHandlerFactory<MagicLinkActionToken> {

  public static final String PROVIDER_ID = "magic-link-token-handler";

  @Override
  public MagicLinkActionTokenHandler create(KeycloakSession session) {
    return new MagicLinkActionTokenHandler();
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
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
}
