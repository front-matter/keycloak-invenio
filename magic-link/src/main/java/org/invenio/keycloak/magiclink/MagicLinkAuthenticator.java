package org.invenio.keycloak.magiclink;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Magic Link Authenticator - sends email with one-time passwordless login link
 */
public class MagicLinkAuthenticator implements Authenticator {

  private static final String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    // Show email input form
    Response challenge = context.form().createLoginUsername();
    context.challenge(challenge);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    String email = formData.getFirst("username");

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
        user = createUser(context, email);
      } else {
        // Don't reveal user doesn't exist
        showEmailSentPage(context);
        return;
      }
    }

    // Check if user is enabled
    if (!user.isEnabled()) {
      context.getEvent().user(user).error(Errors.USER_DISABLED);
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

  private String generateMagicLink(AuthenticationFlowContext context, UserModel user) {
    int validityInSecs = getTokenValidity(context);
    int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

    String clientId = context.getSession().getContext().getClient().getClientId();
    String redirectUri = context.getAuthenticationSession().getRedirectUri();
    Boolean rememberMe = false;

    // Get compound authentication session ID to preserve OIDC context
    String authSessionId = null;
    if (context.getAuthenticationSession() != null
        && context.getAuthenticationSession().getParentSession() != null) {
      authSessionId = context.getAuthenticationSession().getParentSession().getId()
          + "." + context.getAuthenticationSession().getTabId();
      org.jboss.logging.Logger.getLogger(getClass()).debugf(
          "Magic Link: Using auth session ID: %s", authSessionId);
    } else {
      org.jboss.logging.Logger.getLogger(getClass()).warn(
          "Magic Link: No parent session found - proceeding without session ID");
    }

    org.jboss.logging.Logger.getLogger(getClass()).debugf(
        "Magic Link: Generating token - userId=%s, clientId=%s, redirectUri=%s, authSessionId=%s, validitySecs=%d, absoluteExp=%d",
        user.getId(), clientId, redirectUri, authSessionId, validityInSecs, absoluteExpirationInSecs);

    MagicLinkActionToken token = new MagicLinkActionToken(
        user.getId(),
        absoluteExpirationInSecs,
        clientId,
        redirectUri,
        rememberMe,
        authSessionId);

    org.jboss.logging.Logger.getLogger(getClass()).debugf(
        "Magic Link: Token created, now serializing - tokenId=%s, nonce=%s, tokenType=%s",
        token.getId(), token.getActionVerificationNonce(), token.getType());

    UriInfo uriInfo = context.getSession().getContext().getUri();
    String tokenString = token.serialize(
        context.getSession(),
        context.getRealm(),
        uriInfo);

    org.jboss.logging.Logger.getLogger(getClass()).debugf(
        "Magic Link: Token serialized successfully - length=%d, userId=%s, tokenStringPrefix=%s",
        tokenString != null ? tokenString.length() : 0, user.getId(),
        tokenString != null && tokenString.length() > 50 ? tokenString.substring(0, 50) : tokenString);

    UriBuilder builder = Urls.realmBase(uriInfo.getBaseUri())
        .path(RealmsResource.class, "getLoginActionsService")
        .path(LoginActionsService.class, "executeActionToken")
        .queryParam("key", tokenString)
        .queryParam("client_id", clientId);

    String link = builder.build(context.getRealm().getName()).toString();
    org.jboss.logging.Logger.getLogger(getClass()).debugf(
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
    Response challenge = context.form()
        .createForm("magic-link-sent.ftl");
    context.challenge(challenge);
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
