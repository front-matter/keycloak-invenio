package org.invenio.keycloak.magiclink;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Handles the magic link action token by logging the user in and redirecting
 */
public class MagicLinkActionTokenHandler extends AbstractActionTokenHandler<MagicLinkActionToken> {

  public static final String LOGIN_METHOD = "login_method";

  public MagicLinkActionTokenHandler() {
    super(
        MagicLinkActionToken.TOKEN_TYPE,
        MagicLinkActionToken.class,
        Messages.INVALID_REQUEST,
        EventType.EXECUTE_ACTION_TOKEN,
        Errors.INVALID_REQUEST);
  }

  @Override
  public AuthenticationSessionModel startFreshAuthenticationSession(
      MagicLinkActionToken token,
      ActionTokenContext<MagicLinkActionToken> tokenContext) {
    return tokenContext.createAuthenticationSessionForClient(token.getIssuedFor());
  }

  @Override
  public Response handleToken(
      MagicLinkActionToken token,
      ActionTokenContext<MagicLinkActionToken> tokenContext) {

    UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
    AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
    ClientModel client = authSession.getClient();

    // Get redirect URI
    String redirectUri = token.getRedirectUri() != null
        ? token.getRedirectUri()
        : ResolveRelative.resolveRelativeUri(
            tokenContext.getSession(),
            client.getRootUrl(),
            client.getBaseUrl());

    // Validate redirect URI
    String redirect = RedirectUtils.verifyRedirectUri(
        tokenContext.getSession(),
        redirectUri,
        client);

    if (redirect != null) {
      authSession.setAuthNote(
          AuthenticationManager.SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS,
          "true");
      authSession.setRedirectUri(redirect);
      authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);

      // Remember me functionality
      if (Boolean.TRUE.equals(token.getRememberMe())) {
        authSession.setAuthNote(
            AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION,
            "auth_remember");
      }

      // Mark email as verified (user clicked link in email)
      user.setEmailVerified(true);

      // Set user session note to track magic link login
      authSession.setUserSessionNote(LOGIN_METHOD, "magic-link");

      // Check for required actions
      String nextAction = AuthenticationManager.nextRequiredAction(
          tokenContext.getSession(),
          authSession,
          tokenContext.getRequest(),
          tokenContext.getEvent());

      return AuthenticationManager.redirectToRequiredActions(
          tokenContext.getSession(),
          tokenContext.getRealm(),
          authSession,
          tokenContext.getUriInfo(),
          nextAction);
    }

    // Invalid redirect URI
    tokenContext.getEvent()
        .detail("redirect_uri", redirectUri)
        .error(Errors.INVALID_REDIRECT_URI);
    return tokenContext.getSession().getProvider(org.keycloak.forms.login.LoginFormsProvider.class)
        .setError(Messages.INVALID_REDIRECT_URI)
        .createErrorPage(Response.Status.BAD_REQUEST);
  }
}
