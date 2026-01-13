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

  // Required for Keycloak to deserialize tokens to the correct class (called via
  // reflection)
  public Class<MagicLinkActionToken> getTokenClass() {
    logger.infof("Magic Link: getTokenClass() called - returning: %s", MagicLinkActionToken.class.getName());
    return MagicLinkActionToken.class;
  }

  @Override
  public void init(Config.Scope config) {
    logger.infof("Magic Link: Handler factory initialized - ID: %s, TokenType: %s, TokenClass: %s, HandlerClass: %s",
        PROVIDER_ID, MagicLinkActionToken.TOKEN_TYPE, MagicLinkActionToken.class.getName(),
        MagicLinkActionTokenHandler.class.getName());
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    logger.infof("Magic Link: Handler factory post-initialization completed - will handle token type: %s",
        MagicLinkActionToken.TOKEN_TYPE);
  }

  @Override
  public void close() {
    // Nothing to close
  }
}
