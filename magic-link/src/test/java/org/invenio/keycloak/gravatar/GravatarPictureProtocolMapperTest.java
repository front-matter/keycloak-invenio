package org.invenio.keycloak.gravatar;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GravatarPictureProtocolMapperTest {

  @Test
  void gravatarUrl_isComputedFromNormalizedEmail() {
    String url = GravatarPictureProtocolMapper.gravatarUrlForEmail(
        " MyEmailAddress@example.com ",
        200,
        "mp",
        "g");

    assertNotNull(url);
    assertTrue(url.startsWith("https://www.gravatar.com/avatar/"));
    assertTrue(url.contains("0bc83cb571cd1c50ba6f3e8a78ef1346"), url);
    assertTrue(url.contains("s=200"), url);
    assertTrue(url.contains("d=mp"), url);
    assertTrue(url.contains("r=g"), url);
  }

  @Test
  void gravatarUrl_nullForMissingEmail() {
    assertNull(GravatarPictureProtocolMapper.gravatarUrlForEmail(null, 200, "mp", "g"));
    assertNull(GravatarPictureProtocolMapper.gravatarUrlForEmail("   ", 200, "mp", "g"));
  }
}
