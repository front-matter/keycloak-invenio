package org.invenio.keycloak.magiclink;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.*;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailSenderProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MagicLinkAuthenticatorTest {

    @Mock
    private AuthenticationFlowContext context;

    @Mock
    private KeycloakSession session;

    @Mock
    private RealmModel realm;

    @Mock
    private UserModel user;

    @Mock
    private ClientModel client;

    @Mock
    private LoginFormsProvider loginFormsProvider;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private AuthenticationSessionModel authSession;

    @Mock
    private EventBuilder eventBuilder;

    private MagicLinkAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new MagicLinkAuthenticator();
        lenient().when(context.form()).thenReturn(loginFormsProvider);
        lenient().when(context.getSession()).thenReturn(session);
        lenient().when(context.getRealm()).thenReturn(realm);
        lenient().when(context.getAuthenticationSession()).thenReturn(authSession);
        lenient().when(context.getEvent()).thenReturn(eventBuilder);

        // Used by showEmailSentPage() (domain auto-creation tests hit this path)
        lenient().when(loginFormsProvider.setError(anyString())).thenReturn(loginFormsProvider);
        lenient().when(loginFormsProvider.setInfo(anyString())).thenReturn(loginFormsProvider);
        lenient().when(loginFormsProvider.createErrorPage(any()))
                .thenReturn(Response.status(Response.Status.UNAUTHORIZED).build());
        lenient().when(loginFormsProvider.createInfoPage())
                .thenReturn(Response.ok().build());
        lenient().when(loginFormsProvider.createForm(anyString()))
                .thenReturn(Response.ok().build());

        // Used by createUser() and disabled-user handling
        lenient().when(eventBuilder.user(any(UserModel.class))).thenReturn(eventBuilder);
        lenient().when(eventBuilder.detail(anyString(), anyString())).thenReturn(eventBuilder);
        lenient().when(eventBuilder.event(any())).thenReturn(eventBuilder);
    }

    @Test
    void testAuthenticate() {
        when(loginFormsProvider.createLoginUsername()).thenReturn(Response.ok().build());

        authenticator.authenticate(context);

        verify(context).form();
        verify(loginFormsProvider).createLoginUsername();
        verify(context).challenge(any(Response.class));
    }

    @Test
    void testActionWithMissingEmail() {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.putSingle("username", "");

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(loginFormsProvider.setError(anyString())).thenReturn(loginFormsProvider);
        when(loginFormsProvider.createLoginUsername()).thenReturn(Response.ok().build());

        authenticator.action(context);

        verify(context).failureChallenge(any(), any(Response.class));
        verify(eventBuilder).error(anyString());
    }

    @Test
    void testRequiresUser() {
        assertFalse(authenticator.requiresUser());
    }

    @Test
    void testConfiguredFor() {
        assertTrue(authenticator.configuredFor(session, realm, user));
    }

    @Test
    void testSetRequiredActions() {
        authenticator.setRequiredActions(session, realm, user);
    }

    @Test
    void testClose() {
        authenticator.close();
    }

    @Test
    void testMagicLinkSubjectContainsRealmName() throws Exception {
        // Arrange
        String testRealm = "TestRealm";
        String testEmail = "user@example.com";
        UserModel mockUser = mock(UserModel.class);
        EmailTemplateProvider emailProvider = mock(EmailTemplateProvider.class);
        KeycloakSession mockSession = mock(KeycloakSession.class);
        AuthenticationFlowContext mockContext = mock(AuthenticationFlowContext.class);
        RealmModel mockRealm = mock(RealmModel.class);

        when(mockRealm.getDisplayName()).thenReturn(testRealm);
        when(mockContext.getRealm()).thenReturn(mockRealm);
        when(mockContext.getSession()).thenReturn(mockSession);
        when(mockSession.getProvider(EmailTemplateProvider.class)).thenReturn(emailProvider);

        // Capture subject argument
        doAnswer(invocation -> {
            String subjectKey = invocation.getArgument(0);
            List<?> subjectParams = invocation.getArgument(1);
            Map<String, Object> attrs = invocation.getArgument(3);
            // Verify realm name is passed as parameter
            assertNotNull(subjectParams, "Subject parameters should not be null");
            assertFalse(subjectParams.isEmpty(), "Subject parameters should contain realm name");
            assertEquals(testRealm, subjectParams.get(0), "First parameter should be realm name");
            return null;
        }).when(emailProvider).send(anyString(), anyList(), anyString(), anyMap());

        // Act
        MagicLinkAuthenticator authenticator = new MagicLinkAuthenticator();
        authenticator.sendMagicLinkEmail(mockContext, mockUser, "https://example.com/magic-link");
    }

    @Test
    void testDomainBasedAutoCreation_AllowedDomain() {
        // Setup
        String email = "user@example.com";
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.putSingle("username", email);

        AuthenticatorConfigModel config = mock(AuthenticatorConfigModel.class);
        Map<String, String> configMap = new HashMap<>();
        configMap.put("allowedDomainsGroup", "auto-create-domains");
        configMap.put("createUser", "false");

        GroupModel group = mock(GroupModel.class);
        UserProvider userProvider = mock(UserProvider.class);

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(context.getAuthenticatorConfig()).thenReturn(config);
        when(config.getConfig()).thenReturn(configMap);
        when(context.getRealm()).thenReturn(realm);
        // Return a fresh stream on every call: isDomainAllowed is invoked twice
        // (early domain check + shouldCreateUserByDomain) when the domain is allowed.
        when(realm.getGroupsStream()).thenAnswer(inv -> Stream.of(group));
        when(group.getName()).thenReturn("auto-create-domains");
        when(group.getAttributeStream("allowed-domains"))
                .thenAnswer(inv -> Stream.of("example.com", "company.org"));
        when(session.users()).thenReturn(userProvider);

        // Mock username uniqueness check (no collision)
        when(userProvider.getUserByUsername(any(), anyString())).thenReturn(null);

        // Accept any generated username (usr_xxx format)
        when(userProvider.addUser(any(), anyString())).thenReturn(user);

        // Stop the flow before token generation: mock stays disabled even after
        // setEnabled(true)
        when(user.isEnabled()).thenReturn(false);

        // Execute
        authenticator.action(context);

        // Verify user was created with generated username (not email)
        verify(userProvider).addUser(eq(realm), argThat(username -> username != null && username.startsWith("usr_")));
        verify(user).setEnabled(true);
        verify(user).setEmail(email);
        verify(user).setEmailVerified(true);

        // Verify required actions are explicitly removed (firstName/lastName are
        // optional)
        verify(user).removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        verify(user).removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        verify(user).removeRequiredAction("VERIFY_PROFILE"); // User Profile feature in Keycloak 26.x

        // The flow continues to the generic "email sent" page (challenge keeps session
        // alive)
        verify(context).challenge(any(Response.class));
    }

    @Test
    void testDomainBasedAutoCreation_DisallowedDomain() {
        // Setup
        String email = "user@untrusted.com";
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.putSingle("username", email);

        AuthenticatorConfigModel config = mock(AuthenticatorConfigModel.class);
        Map<String, String> configMap = new HashMap<>();
        configMap.put("allowedDomainsGroup", "auto-create-domains");
        configMap.put("createUser", "false");

        GroupModel group = mock(GroupModel.class);
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(context.getAuthenticatorConfig()).thenReturn(config);
        when(config.getConfig()).thenReturn(configMap);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getGroupsStream()).thenReturn(Stream.of(group));
        when(group.getName()).thenReturn("auto-create-domains");
        when(group.getAttributeStream("allowed-domains"))
                .thenReturn(Stream.of("example.com", "company.org"));
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);
        when(realm.getSmtpConfig()).thenReturn(new HashMap<>());

        // Execute
        authenticator.action(context);

        // Verify notification email was sent to the user's address
        try {
            verify(emailSender).send(any(), eq(email), anyString(), anyString(), anyString());
        } catch (org.keycloak.email.EmailException e) {
            fail("EmailException should not be thrown in verify: " + e.getMessage());
        }
        // Should show email sent page
        verify(context).challenge(any(Response.class));
    }

    @Test
    void testDomainBasedAutoCreation_GroupNotFound() {
        // Setup
        String email = "user@example.com";
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.putSingle("username", email);

        AuthenticatorConfigModel config = mock(AuthenticatorConfigModel.class);
        Map<String, String> configMap = new HashMap<>();
        configMap.put("allowedDomainsGroup", "nonexistent-group");
        configMap.put("createUser", "false");

        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(context.getAuthenticatorConfig()).thenReturn(config);
        when(config.getConfig()).thenReturn(configMap);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getGroupsStream()).thenReturn(Stream.empty());
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);
        when(realm.getSmtpConfig()).thenReturn(new HashMap<>());

        // Execute
        authenticator.action(context);

        // Should show email sent page (group misconfigured → domain treated as not
        // allowed)
        verify(context).challenge(any(Response.class));
    }

    // --- sendDomainNotAllowedEmail tests ---

    @Test
    void testSendDomainNotAllowedEmail_usesReplyToWhenSet() throws Exception {
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = new HashMap<>();
        smtp.put("from", "noreply@example.com");
        smtp.put("replyTo", "support@example.com");

        when(context.getRealm()).thenReturn(realm);
        when(realm.getDisplayName()).thenReturn("My Platform");
        when(realm.getSmtpConfig()).thenReturn(smtp);
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);

        authenticator.sendDomainNotAllowedEmail(context, "user@restricted.org");

        // sent to the user
        verify(emailSender).send(
                eq(smtp),
                eq("user@restricted.org"),
                anyString(),
                argThat(body -> body.contains("support@example.com")),
                anyString());
        // CC copy sent to support address, body includes the attempted email
        verify(emailSender).send(
                eq(smtp),
                eq("support@example.com"),
                anyString(),
                argThat(body -> body.contains("user@restricted.org")),
                anyString());
    }

    @Test
    void testSendDomainNotAllowedEmail_fallsBackToFromWhenReplyToEmpty() throws Exception {
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = new HashMap<>();
        smtp.put("from", "noreply@example.com");
        smtp.put("replyTo", "");

        when(context.getRealm()).thenReturn(realm);
        when(realm.getDisplayName()).thenReturn("My Platform");
        when(realm.getSmtpConfig()).thenReturn(smtp);
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);

        authenticator.sendDomainNotAllowedEmail(context, "user@restricted.org");

        // sent to the user
        verify(emailSender).send(
                eq(smtp),
                eq("user@restricted.org"),
                anyString(),
                argThat(body -> body.contains("noreply@example.com")),
                anyString());
        // CC copy sent to from address (replyTo was empty), body includes the attempted
        // email
        verify(emailSender).send(
                eq(smtp),
                eq("noreply@example.com"),
                anyString(),
                argThat(body -> body.contains("user@restricted.org")),
                anyString());
    }

    @Test
    void testSendDomainNotAllowedEmail_fallsBackToFromWhenReplyToAbsent() throws Exception {
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = new HashMap<>();
        smtp.put("from", "noreply@example.com");

        when(context.getRealm()).thenReturn(realm);
        when(realm.getDisplayName()).thenReturn("My Platform");
        when(realm.getSmtpConfig()).thenReturn(smtp);
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);

        authenticator.sendDomainNotAllowedEmail(context, "user@restricted.org");

        // sent to the user
        verify(emailSender).send(
                eq(smtp),
                eq("user@restricted.org"),
                anyString(),
                argThat(body -> body.contains("noreply@example.com")),
                anyString());
        // CC copy sent to from address (replyTo absent), body includes the attempted
        // email
        verify(emailSender).send(
                eq(smtp),
                eq("noreply@example.com"),
                anyString(),
                argThat(body -> body.contains("user@restricted.org")),
                anyString());
    }

    @Test
    void testSendDomainNotAllowedEmail_bodyContainsRealmName() throws Exception {
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = new HashMap<>();
        smtp.put("from", "noreply@example.com");

        when(context.getRealm()).thenReturn(realm);
        when(realm.getDisplayName()).thenReturn("Engineering Edge");
        when(realm.getSmtpConfig()).thenReturn(smtp);
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);

        authenticator.sendDomainNotAllowedEmail(context, "user@restricted.org");

        verify(emailSender).send(
                eq(smtp),
                eq("user@restricted.org"),
                anyString(),
                argThat(body -> body.contains("Engineering Edge")),
                anyString());
    }

    @Test
    void testSendDomainNotAllowedEmail_noCcWhenRecipientMatchesCcAddress() throws Exception {
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = new HashMap<>();
        smtp.put("from", "user@restricted.org"); // same as recipient

        when(context.getRealm()).thenReturn(realm);
        when(realm.getDisplayName()).thenReturn("My Platform");
        when(realm.getSmtpConfig()).thenReturn(smtp);
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);

        authenticator.sendDomainNotAllowedEmail(context, "user@restricted.org");

        // Only one send: no CC to avoid duplicate delivery to the same address
        verify(emailSender, times(1)).send(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testSendDomainNotAllowedEmail_emailExceptionIsSuppressed() throws Exception {
        EmailSenderProvider emailSender = mock(EmailSenderProvider.class);
        Map<String, String> smtp = new HashMap<>();
        smtp.put("from", "noreply@example.com");

        when(context.getRealm()).thenReturn(realm);
        when(realm.getDisplayName()).thenReturn("My Platform");
        when(realm.getSmtpConfig()).thenReturn(smtp);
        when(session.getProvider(EmailSenderProvider.class)).thenReturn(emailSender);
        doThrow(new org.keycloak.email.EmailException("SMTP error"))
                .when(emailSender).send(any(), anyString(), anyString(), anyString(), anyString());

        // Must not throw
        assertDoesNotThrow(() -> authenticator.sendDomainNotAllowedEmail(context, "user@restricted.org"));
    }
}
