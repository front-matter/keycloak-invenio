package org.invenio.keycloak.magiclink;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for Magic Link Action Token Handler
 */
public class MagicLinkActionTokenHandlerFactory implements ActionTokenHandlerFactory<MagicLinkActionToken> {

  private static final Logger logger = Logger.getLogger(MagicLinkActionTokenHandlerFactory.class);
  public static final String PROVIDER_ID = "magic-link-token-handler";

  @Override
  public MagicLinkActionTokenHandler create(KeycloakSession session) {
    logger.info("Magic Link: Creating MagicLinkActionTokenHandler instance");
    return new MagicLinkActionTokenHandler();
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public void init(Config.Scope config) {
    logger.infof("Magic Link: Handler factory initialized with ID: %s", PROVIDER_ID);
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    logger.info("Magic Link: Handler factory post-initialization completed");
  }

  @Override
  public void close() {
    // Nothing to close
  }
}
