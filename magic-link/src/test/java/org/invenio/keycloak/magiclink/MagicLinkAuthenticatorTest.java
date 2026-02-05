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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        lenient().when(loginFormsProvider.createErrorPage(any()))
                .thenReturn(Response.status(Response.Status.UNAUTHORIZED).build());

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
        when(realm.getGroupsStream()).thenReturn(Stream.of(group));
        when(group.getName()).thenReturn("auto-create-domains");
        when(group.getAttributeStream("allowed-domains"))
                .thenReturn(Stream.of("example.com", "company.org"));
        when(session.users()).thenReturn(userProvider);
        when(userProvider.addUser(realm, email)).thenReturn(user);

        // Stop the flow before token generation: mock stays disabled even after
        // setEnabled(true)
        when(user.isEnabled()).thenReturn(false);

        // Execute
        authenticator.action(context);

        // Verify user was created
        verify(userProvider).addUser(realm, email);
        verify(user).setEnabled(true);
        verify(user).setEmail(email);

        // The flow continues to the generic "email sent" page
        verify(context).failure(any(), any(Response.class));
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
        UserProvider userProvider = mock(UserProvider.class);

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(context.getAuthenticatorConfig()).thenReturn(config);
        when(config.getConfig()).thenReturn(configMap);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getGroupsStream()).thenReturn(Stream.of(group));
        when(group.getName()).thenReturn("auto-create-domains");
        when(group.getAttributeStream("allowed-domains"))
                .thenReturn(Stream.of("example.com", "company.org"));
        when(session.users()).thenReturn(userProvider);

        // Execute
        authenticator.action(context);

        // Verify user was NOT created
        verify(userProvider, never()).addUser(any(), anyString());
        // Should show email sent page without revealing user doesn't exist
        verify(context).failure(any(), any(Response.class));
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

        UserProvider userProvider = mock(UserProvider.class);

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(context.getAuthenticatorConfig()).thenReturn(config);
        when(config.getConfig()).thenReturn(configMap);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getGroupsStream()).thenReturn(Stream.empty());
        when(session.users()).thenReturn(userProvider);

        // Execute
        authenticator.action(context);

        // Verify user was NOT created
        verify(userProvider, never()).addUser(any(), anyString());
        verify(context).failure(any(), any(Response.class));
    }
}
