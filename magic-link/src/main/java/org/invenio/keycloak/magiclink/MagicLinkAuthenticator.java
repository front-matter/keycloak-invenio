package org.invenio.keycloak.magiclink;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Magic Link Authenticator - sends email with one-time passwordless login link
 */
public class MagicLinkAuthenticator implements Authenticator {

  private static final Logger logger = Logger.getLogger(MagicLinkAuthenticator.class);

  private static final String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    logContextInfo("authenticate", context, null);
    // Show email input form
    Response challenge = context.form().createLoginUsername();
    context.challenge(challenge);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    String email = formData.getFirst("username");

    // Helpful when templates post back (e.g. resend/restart)
    String restart = formData.getFirst("restart");
    logger.infof(
        "Magic Link: action() called - restart=%s, usernamePresent=%s",
        restart,
        email != null && !email.trim().isEmpty());
    logContextInfo("action", context, email);

    if (email == null || email.trim().isEmpty()) {
      context.getEvent().error(Errors.USERNAME_MISSING);
      Response challenge = context.form()
          .setError(Messages.MISSING_USERNAME)
          .createLoginUsername();
      context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
      return;
    }

    email = email.trim().toLowerCase();

    // Find or create user
    UserModel user = KeycloakModelUtils.findUserByNameOrEmail(
        context.getSession(),
        context.getRealm(),
        email);

    if (user == null) {
      // User doesn't exist - optionally create
      if (shouldCreateUser(context)) {
        logger.debugf("Magic Link: User not found, auto-create enabled - email=%s", email);
        user = createUser(context, email);
      } else {
        logger.debugf("Magic Link: User not found, auto-create disabled - email=%s", email);
        // Don't reveal user doesn't exist
        showEmailSentPage(context);
        return;
      }
    }

    // Check if user is enabled
    if (!user.isEnabled()) {
      context.getEvent().user(user).error(Errors.USER_DISABLED);
      logger.debugf("Magic Link: User disabled - userId=%s, email=%s", user.getId(), email);
      showEmailSentPage(context);
      return;
    }

    // Generate and send magic link
    try {
      String link = generateMagicLink(context, user);
      sendMagicLinkEmail(context, user, link);

      // Store username for potential next steps
      context.getAuthenticationSession()
          .setAuthNote(ATTEMPTED_USERNAME, email);

      // Set user in session so the authentication can be completed when they click
      // the link
      context.getAuthenticationSession().setAuthNote("MAGIC_LINK_SENT", "true");

      logger.debugf(
          "Magic Link: Email send attempted - userId=%s, clientId=%s, redirectUri=%s",
          user.getId(),
          safeClientId(context),
          safeRedirectUri(context));

      showEmailSentPage(context);
    } catch (EmailException e) {
      // Log detailed email error
      context.getEvent().error(Errors.EMAIL_SEND_FAILED);
      context.getEvent().detail("email_error", e.getMessage());
      org.jboss.logging.Logger.getLogger(getClass()).error("Failed to send magic link email to: " + email, e);

      Response challenge = context.form()
          .setError("Failed to send email. Please try again.")
          .createLoginUsername();
      context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
    } catch (Exception e) {
      // Log unexpected errors
      org.jboss.logging.Logger.getLogger(getClass()).error("Unexpected error during magic link generation", e);
      context.getEvent().error(Errors.EMAIL_SEND_FAILED);

      Response challenge = context.form()
          .setError("Failed to send email. Please try again.")
          .createLoginUsername();
      context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
    }
  }

  private static String safeClientId(AuthenticationFlowContext context) {
    try {
      return context.getSession().getContext().getClient() != null
          ? context.getSession().getContext().getClient().getClientId()
          : "null";
    } catch (Exception e) {
      return "unknown";
    }
  }

  private static String safeRedirectUri(AuthenticationFlowContext context) {
    try {
      return context.getAuthenticationSession() != null ? context.getAuthenticationSession().getRedirectUri() : "null";
    } catch (Exception e) {
      return "unknown";
    }
  }

  private static void logContext(String phase, AuthenticationFlowContext context, String email) {
    try {
      URI requestUri = null;
      try {
        if (context != null && context.getSession() != null && context.getSession().getContext() != null) {
          UriInfo uriInfo = context.getSession().getContext().getUri();
          requestUri = uriInfo != null ? uriInfo.getRequestUri() : null;
        }
      } catch (Exception ignored) {
        // Best-effort only
      }

      AuthenticationSessionModel authSession = context != null ? context.getAuthenticationSession() : null;

      String parentSessionId = "null";
      String tabId = "null";
      if (authSession != null) {
        tabId = authSession.getTabId();
        parentSessionId = authSession.getParentSession() != null ? authSession.getParentSession().getId() : "null";
      }

      logger.debugf(
          "Magic Link: %s - requestUri=%s, realm=%s, clientId=%s, redirectUri=%s, authSession=%s.%s, email=%s",
          phase,
          requestUri,
          context.getRealm() != null ? context.getRealm().getName() : "null",
          safeClientId(context),
          safeRedirectUri(context),
          parentSessionId,
          tabId,
          email != null ? email : "null");
    } catch (Exception e) {
      logger.debugf(e, "Magic Link: %s - failed to log context", phase);
    }
  }

  /**
   * INFO-level flow marker: enough context to correlate in production logs.
   */
  private static void logContextInfo(String phase, AuthenticationFlowContext context, String email) {
    try {
      URI requestUri = null;
      try {
        if (context != null && context.getSession() != null && context.getSession().getContext() != null) {
          UriInfo uriInfo = context.getSession().getContext().getUri();
          requestUri = uriInfo != null ? uriInfo.getRequestUri() : null;
        }
      } catch (Exception ignored) {
        // Best-effort only
      }

      AuthenticationSessionModel authSession = context != null ? context.getAuthenticationSession() : null;

      String parentSessionId = "null";
      String tabId = "null";
      if (authSession != null) {
        tabId = authSession.getTabId();
        parentSessionId = authSession.getParentSession() != null ? authSession.getParentSession().getId() : "null";
      }

      logger.infof(
          "Magic Link: %s - requestUri=%s, realm=%s, clientId=%s, redirectUri=%s, authSession=%s.%s, email=%s",
          phase,
          requestUri,
          context.getRealm() != null ? context.getRealm().getName() : "null",
          safeClientId(context),
          safeRedirectUri(context),
          parentSessionId,
          tabId,
          email != null ? email : "null");
    } catch (Exception e) {
      // Avoid noisy INFO logs in partially mocked environments; DEBUG has details.
      logger.debugf(e, "Magic Link: %s - failed to log context", phase);
    }
  }

  private String generateMagicLink(AuthenticationFlowContext context, UserModel user) {
    int validityInSecs = getTokenValidity(context);
    int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

    String clientId = context.getSession().getContext().getClient().getClientId();
    String redirectUri = context.getAuthenticationSession().getRedirectUri();
    Boolean rememberMe = false;

    // Capture all client notes from original session for OIDC flow preservation
    Map<String, String> clientNotes = new HashMap<>(context.getAuthenticationSession().getClientNotes());

    // Don't pass compound session ID - the original auth session may expire before
    // the user clicks the link. Keycloak will create a fresh authentication session
    // when processing the token, using the clientId and redirectUri stored in the
    // token.
    logger.debugf(
        "Magic Link: Generating token - userId=%s, clientId=%s, redirectUri=%s, clientNotes=%d, validitySecs=%d, absoluteExp=%d",
        user.getId(), clientId, redirectUri, clientNotes.size(), validityInSecs, absoluteExpirationInSecs);

    MagicLinkActionToken token = new MagicLinkActionToken(
        user.getId(),
        absoluteExpirationInSecs,
        clientId,
        redirectUri,
        rememberMe,
        null, // null = create fresh auth session
        clientNotes);

    logger.debugf(
        "Magic Link: Token created, now serializing - tokenId=%s, nonce=%s, tokenType=%s",
        token.getId(), token.getActionVerificationNonce(), token.getType());

    UriInfo uriInfo = context.getSession().getContext().getUri();
    String tokenString = token.serialize(
        context.getSession(),
        context.getRealm(),
        uriInfo);

    // Don't log the token itself (it can be used to log in). Log only a small
    // fingerprint.
    String tokenFingerprint = tokenString == null ? "null" : Integer.toHexString(tokenString.hashCode());
    logger.debugf(
        "Magic Link: Token serialized - length=%d, userId=%s, tokenFingerprint=%s",
        tokenString != null ? tokenString.length() : 0,
        user.getId(),
        tokenFingerprint);

    UriBuilder builder = Urls.realmBase(uriInfo.getBaseUri())
        .path(RealmsResource.class, "getLoginActionsService")
        .path(LoginActionsService.class, "executeActionToken")
        .queryParam("key", tokenString)
        .queryParam("client_id", clientId);

    String link = builder.build(context.getRealm().getName()).toString();
    logger.debugf(
        "Magic Link: Link generated - length=%d, userId=%s",
        link.length(), user.getId());

    return link;
  }

  protected void sendMagicLinkEmail(AuthenticationFlowContext context, UserModel user, String link)
      throws EmailException {
    EmailTemplateProvider emailProvider = context.getSession().getProvider(EmailTemplateProvider.class);
    emailProvider.setRealm(context.getRealm());
    emailProvider.setUser(user);

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("link", link);
    attributes.put("linkExpiration", getTokenValidity(context) / 60); // minutes
    attributes.put("realmName", context.getRealm().getDisplayName());

    emailProvider.send(
        "magicLinkSubject",
        Arrays.asList(context.getRealm().getDisplayName()),
        "magic-link.ftl",
        attributes);
  }

  private UserModel createUser(AuthenticationFlowContext context, String email) {
    UserModel user = context.getSession().users().addUser(context.getRealm(), email);
    user.setEnabled(true);
    user.setEmail(email);
    context.getEvent()
        .user(user)
        .detail("username", email)
        .event(EventType.REGISTER);
    return user;
  }

  private void showEmailSentPage(AuthenticationFlowContext context) {
    // For OAuth flows, we need to properly terminate the authorization request.
    // Since magic link auth is asynchronous (user must click email), we can't
    // complete
    // the current OAuth flow. Instead, we return an error response that redirects
    // back
    // to the client application with an error message.
    // When the user clicks the magic link, a NEW auth session is created and
    // succeeds.

    logger.infof(
        "Magic Link: Terminating current auth flow (async magic link) - clientId=%s, redirectUri=%s",
        safeClientId(context),
        safeRedirectUri(context));

    context.getEvent().error("magic_link_sent");
    Response response = context.form()
        .setError(
            "A login link has been sent to your email. Please check your inbox and click the link to continue. You can close this window.")
        .createErrorPage(Response.Status.UNAUTHORIZED);
    context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, response);
  }

  private boolean shouldCreateUser(AuthenticationFlowContext context) {
    return context.getAuthenticatorConfig() != null
        && Boolean.parseBoolean(
            context.getAuthenticatorConfig()
                .getConfig()
                .getOrDefault("createUser", "false"));
  }

  private int getTokenValidity(AuthenticationFlowContext context) {
    if (context.getAuthenticatorConfig() != null) {
      String validity = context.getAuthenticatorConfig()
          .getConfig()
          .get("tokenValidity");
      if (validity != null) {
        try {
          return Integer.parseInt(validity);
        } catch (NumberFormatException e) {
          // Fall through to default
        }
      }
    }
    return 900; // Default: 15 minutes
  }

  @Override
  public boolean requiresUser() {
    return false;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true;
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    // No required actions
  }

  @Override
  public void close() {
    // Nothing to close
  }
}
