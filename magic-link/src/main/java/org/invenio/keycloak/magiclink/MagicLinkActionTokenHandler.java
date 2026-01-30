package org.invenio.keycloak.magiclink;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.authentication.actiontoken.TokenUtils;
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

import java.util.Map;

/**
 * Handles the magic link action token by logging the user in and redirecting
 */
public class MagicLinkActionTokenHandler extends AbstractActionTokenHandler<MagicLinkActionToken> {

        private static final Logger logger = Logger.getLogger(MagicLinkActionTokenHandler.class);
        public static final String LOGIN_METHOD = "login_method";

        public MagicLinkActionTokenHandler() {
                super(
                                MagicLinkActionToken.TOKEN_TYPE,
                                MagicLinkActionToken.class,
                                Messages.INVALID_REQUEST,
                                EventType.EXECUTE_ACTION_TOKEN,
                                Errors.INVALID_REQUEST);
                logger.debugf("Magic Link: Handler constructor called - registering for tokenType: %s, tokenClass: %s",
                                MagicLinkActionToken.TOKEN_TYPE, MagicLinkActionToken.class.getName());
        }

        @Override
        public Predicate<? super MagicLinkActionToken>[] getVerifiers(
                        ActionTokenContext<MagicLinkActionToken> tokenContext) {
                logger.debugf("Magic Link: getVerifiers() called - realm=%s",
                                tokenContext.getRealm().getName());

                AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
                if (authSession != null) {
                        logger.debugf("Magic Link: Current auth session - sessionId=%s.%s, client=%s, user=%s",
                                        authSession.getParentSession() != null ? authSession.getParentSession().getId()
                                                        : "null",
                                        authSession.getTabId(),
                                        authSession.getClient() != null ? authSession.getClient().getClientId()
                                                        : "null",
                                        authSession.getAuthenticatedUser() != null
                                                        ? authSession.getAuthenticatedUser().getId()
                                                        : "null");
                } else {
                        logger.warn("Magic Link: No authentication session found in token context during getVerifiers()");
                }

                // Create custom predicates with logging
                Predicate<? super MagicLinkActionToken>[] basicChecks = TokenUtils.predicates(
                                DefaultActionToken.ACTION_TOKEN_BASIC_CHECKS);

                logger.debugf("Magic Link: Applying %d basic check predicates", basicChecks.length);

                // Wrap each predicate with logging
                @SuppressWarnings("unchecked")
                Predicate<? super MagicLinkActionToken>[] wrappedPredicates = new Predicate[basicChecks.length];
                for (int i = 0; i < basicChecks.length; i++) {
                        final int index = i;
                        final Predicate<? super MagicLinkActionToken> originalPredicate = basicChecks[i];
                        wrappedPredicates[i] = (token) -> {
                                try {
                                        logger.debug(String.format("Magic Link: Running basic check #%d for token: %s",
                                                        index, token));
                                        boolean result = originalPredicate.test(token);
                                        logger.debug(String.format("Magic Link: Basic check #%d result: %s", index,
                                                        result));
                                        return result;
                                } catch (Exception e) {
                                        logger.error(String.format(
                                                        "Magic Link: Basic check #%d failed with exception for token: %s",
                                                        index, token), e);
                                        return false;
                                }
                        };
                }

                return wrappedPredicates;
        }

        @Override
        public AuthenticationSessionModel startFreshAuthenticationSession(
                        MagicLinkActionToken token,
                        ActionTokenContext<MagicLinkActionToken> tokenContext) {
                UriInfo uriInfo = tokenContext.getSession().getContext().getUri();
                logger.infof(
                                "Magic Link: startFreshAuthenticationSession() - requestUri=%s, realm=%s, userId=%s, clientId=%s",
                                uriInfo != null ? uriInfo.getRequestUri() : null,
                                tokenContext.getRealm().getName(),
                                token.getUserId(),
                                token.getIssuedFor());

                logger.debugf("Magic Link: Starting fresh authentication session for userId=%s, clientId=%s, realm=%s",
                                token.getUserId(), token.getIssuedFor(), tokenContext.getRealm().getName());
                // Find client by clientId stored in the token
                ClientModel client = tokenContext.getRealm().getClientByClientId(token.getIssuedFor());
                if (client == null) {
                        logger.warnf("Magic Link: Client not found: clientId=%s, realm=%s",
                                        token.getIssuedFor(), tokenContext.getRealm().getName());
                        return null;
                }
                logger.debugf(
                                "Magic Link: Creating authentication session for client=%s (id=%s), redirectUri=%s, clientNotes=%d",
                                client.getClientId(), client.getId(), token.getRedirectUri(),
                                token.getClientNotes() != null ? token.getClientNotes().size() : 0);

                AuthenticationSessionModel authSession = tokenContext
                                .createAuthenticationSessionForClient(client.getClientId());

                if (authSession != null) {
                        logger.infof(
                                        "Magic Link: Fresh auth session created - parentSessionId=%s, tabId=%s, client=%s",
                                        authSession.getParentSession() != null ? authSession.getParentSession().getId()
                                                        : "null",
                                        authSession.getTabId(),
                                        authSession.getClient() != null ? authSession.getClient().getClientId()
                                                        : "null");
                } else {
                        logger.warn("Magic Link: Failed to create fresh authentication session (authSession=null)");
                }

                if (authSession != null) {
                        // Restore all client notes from token (includes OIDC state, nonce, etc.)
                        if (token.getClientNotes() != null && !token.getClientNotes().isEmpty()) {
                                for (Map.Entry<String, String> entry : token.getClientNotes().entrySet()) {
                                        authSession.setClientNote(entry.getKey(), entry.getValue());
                                        logger.debugf("Magic Link: Restored client note: %s", entry.getKey());
                                }
                        }

                        // Set redirect URI from token if available
                        if (token.getRedirectUri() != null) {
                                String validatedRedirect = RedirectUtils.verifyRedirectUri(
                                                tokenContext.getSession(),
                                                token.getRedirectUri(),
                                                client);
                                if (validatedRedirect != null) {
                                        authSession.setRedirectUri(validatedRedirect);
                                        authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM,
                                                        token.getRedirectUri());
                                        logger.debugf("Magic Link: Set redirect URI in fresh session: %s",
                                                        validatedRedirect);
                                } else {
                                        logger.warnf("Magic Link: Redirect URI validation failed: %s",
                                                        token.getRedirectUri());
                                }
                        }
                }

                return authSession;
        }

        @Override
        public boolean canUseTokenRepeatedly(
                        MagicLinkActionToken token,
                        ActionTokenContext<MagicLinkActionToken> tokenContext) {
                // Magic link tokens are single-use only for security
                logger.debugf("Magic Link: Token is single-use only, userId=%s", token.getUserId());
                return false;
        }

        @Override
        public Response handleToken(
                        MagicLinkActionToken token,
                        ActionTokenContext<MagicLinkActionToken> tokenContext) {

                UriInfo uriInfo = tokenContext.getSession().getContext().getUri();
                logger.infof(
                                "Magic Link: handleToken() request - requestUri=%s, realm=%s",
                                uriInfo != null ? uriInfo.getRequestUri() : null,
                                tokenContext.getRealm().getName());

                logger.debugf(
                                "Magic Link: handleToken() called - userId=%s, clientId=%s, redirectUri=%s, realm=%s, nonce=%s, compoundAuthSessionId=%s, tokenId=%s, exp=%d, iat=%d",
                                token.getUserId(), token.getIssuedFor(), token.getRedirectUri(),
                                tokenContext.getRealm().getName(),
                                token.getActionVerificationNonce(),
                                token.getCompoundAuthenticationSessionId(),
                                token.getId(),
                                token.getExp() != null ? token.getExp() : -1,
                                token.getIat() != null ? token.getIat() : -1);

                // Get user from token (not from session, as they're not authenticated yet)
                UserModel user = tokenContext.getSession().users().getUserById(
                                tokenContext.getRealm(),
                                token.getUserId());

                if (user == null) {
                        logger.warnf("Magic Link: User not found: userId=%s, realm=%s",
                                        token.getUserId(), tokenContext.getRealm().getName());
                        tokenContext.getEvent()
                                        .detail("user_id", token.getUserId())
                                        .error(Errors.USER_NOT_FOUND);
                        return tokenContext.getSession().getProvider(org.keycloak.forms.login.LoginFormsProvider.class)
                                        .setError(Messages.INVALID_USER)
                                        .createErrorPage(Response.Status.BAD_REQUEST);
                }

                // Set user as authenticated in the session
                AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();

                if (authSession == null) {
                        logger.error("Magic Link: No authentication session available in token context during handleToken() (authSession=null)");
                        return tokenContext.getSession().getProvider(org.keycloak.forms.login.LoginFormsProvider.class)
                                        .setError(Messages.INVALID_REQUEST)
                                        .createErrorPage(Response.Status.BAD_REQUEST);
                }

                logger.debugf("Magic Link: Setting authenticated user=%s (email=%s) in session",
                                user.getId(), user.getEmail());
                authSession.setAuthenticatedUser(user);

                ClientModel client = authSession.getClient();
                logger.debugf("Magic Link: Using client=%s (id=%s)", client.getClientId(), client.getId());

                // Get redirect URI
                String redirectUri = token.getRedirectUri() != null
                                ? token.getRedirectUri()
                                : ResolveRelative.resolveRelativeUri(
                                                tokenContext.getSession(),
                                                client.getRootUrl(),
                                                client.getBaseUrl());

                logger.debugf("Magic Link: Redirect URI: %s", redirectUri);

                // Validate redirect URI
                String redirect = RedirectUtils.verifyRedirectUri(
                                tokenContext.getSession(),
                                redirectUri,
                                client);

                if (redirect != null) {
                        logger.debugf("Magic Link: Redirect URI validated successfully: %s", redirect);
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

                        logger.debugf("Magic Link: Redirecting to required actions. nextAction=%s, user=%s",
                                        nextAction != null ? nextAction : "none", user.getId());

                        return AuthenticationManager.redirectToRequiredActions(
                                        tokenContext.getSession(),
                                        tokenContext.getRealm(),
                                        authSession,
                                        tokenContext.getUriInfo(),
                                        nextAction);
                }

                // Invalid redirect URI
                logger.warnf("Magic Link: Invalid redirect URI: %s, client=%s, realm=%s",
                                redirectUri, client.getClientId(), tokenContext.getRealm().getName());
                tokenContext.getEvent()
                                .detail("redirect_uri", redirectUri)
                                .error(Errors.INVALID_REDIRECT_URI);
                return tokenContext.getSession().getProvider(org.keycloak.forms.login.LoginFormsProvider.class)
                                .setError(Messages.INVALID_REDIRECT_URI)
                                .createErrorPage(Response.Status.BAD_REQUEST);
        }
}
