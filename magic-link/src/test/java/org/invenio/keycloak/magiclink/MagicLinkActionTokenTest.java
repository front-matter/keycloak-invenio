package org.invenio.keycloak.magiclink;

import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;

import static org.junit.jupiter.api.Assertions.*;

class MagicLinkActionTokenTest {

  @Test
  void testTokenCreation() {
    String userId = "test-user-123";
    int expirationTime = Time.currentTime() + 3600;
    String clientId = "test-client";
    String redirectUri = "https://example.com/callback";
    Boolean rememberMe = true;
    String authSessionId = "session-123.tab-456";

    MagicLinkActionToken token = new MagicLinkActionToken(
        userId,
        expirationTime,
        clientId,
        redirectUri,
        rememberMe,
        authSessionId);

    assertEquals(userId, token.getUserId());
    assertEquals(clientId, token.getIssuedFor());
    assertEquals(redirectUri, token.getRedirectUri());
    assertEquals(rememberMe, token.getRememberMe());
    assertEquals(MagicLinkActionToken.TOKEN_TYPE, token.getType());
  }

  @Test
  void testTokenWithNullRememberMe() {
    String userId = "test-user";
    int expirationTime = Time.currentTime() + 3600;
    String clientId = "client";
    String redirectUri = "https://example.com";
    String authSessionId = "session-123.tab-456";

    MagicLinkActionToken token = new MagicLinkActionToken(
        userId,
        expirationTime,
        clientId,
        redirectUri,
        null,
        authSessionId);

    assertNotNull(token);
    assertEquals(userId, token.getUserId());
    assertNull(token.getRememberMe());
  }

  @Test
  void testSettersAndGetters() {
    MagicLinkActionToken token = new MagicLinkActionToken(
        "user",
        Time.currentTime() + 1000,
        "client",
        "https://redirect.com",
        false,
        "session-123.tab-456");

    token.setRedirectUri("https://new-redirect.com");
    assertEquals("https://new-redirect.com", token.getRedirectUri());

    token.setRememberMe(true);
    assertTrue(token.getRememberMe());
  }

  @Test
  void testTokenType() {
    assertEquals("magic-link", MagicLinkActionToken.TOKEN_TYPE);
  }
}
