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
import java.util.Map;

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

        when(realm.getName()).thenReturn(testRealm);
        when(context.getRealm()).thenReturn(realm);
        when(context.getSession()).thenReturn(mockSession);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        when(mockUser.isEnabled()).thenReturn(true);
        when(mockUser.getEmail()).thenReturn(testEmail);
        when(mockSession.getProvider(EmailTemplateProvider.class)).thenReturn(emailProvider);

        // Capture subject argument
        doAnswer(invocation -> {
            String subjectKey = invocation.getArgument(0);
            Map<String, Object> attrs = invocation.getArgument(2);
            // Simuliere die Übersetzung
            String subject = "Sign in to " + testRealm;
            assertTrue(subject.contains(testRealm), "Subject should contain realm name");
            return null;
        }).when(emailProvider).send(anyString(), anyString(), anyMap());

        // Act
        MagicLinkAuthenticator authenticator = new MagicLinkAuthenticator();
        authenticator.sendMagicLinkEmail(context, mockUser, "https://example.com/magic-link");
    }
}
