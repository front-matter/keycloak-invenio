package org.invenio.keycloak.magiclink;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UsernameGenerator
 */
class UsernameGeneratorTest {

  @Test
  void testGenerateFormat() {
    String username = UsernameGenerator.generate();

    assertNotNull(username);
    assertTrue(username.startsWith("usr_"), "Username should start with usr_");
    assertEquals(12, username.length(), "Username should be 12 characters total (usr_ + 8 chars)");

    // Check that all characters after usr_ are valid Base32
    String randomPart = username.substring(4);
    assertTrue(randomPart.matches("[0-9a-hj-km-np-tv-z]{8}"),
        "Random part should only contain Base32 characters");
  }

  @Test
  void testGenerateUniqueness() {
    Set<String> usernames = new HashSet<>();
    int count = 1000;

    for (int i = 0; i < count; i++) {
      String username = UsernameGenerator.generate();
      usernames.add(username);
    }

    // With 32^8 possible combinations, 1000 generations should all be unique
    assertEquals(count, usernames.size(), "All generated usernames should be unique");
  }

  @Test
  void testGenerateExcludesAmbiguousCharacters() {
    // Generate many usernames and verify no ambiguous characters
    for (int i = 0; i < 100; i++) {
      String username = UsernameGenerator.generate();
      String randomPart = username.substring(4);

      assertFalse(randomPart.contains("i"), "Should not contain 'i'");
      assertFalse(randomPart.contains("l"), "Should not contain 'l'");
      assertFalse(randomPart.contains("o"), "Should not contain 'o'");
      assertFalse(randomPart.contains("u"), "Should not contain 'u'");
    }
  }

  @Test
  void testGenerateConsistentLength() {
    for (int i = 0; i < 50; i++) {
      String username = UsernameGenerator.generate();
      assertEquals(12, username.length(), "All usernames should be 12 characters");
    }
  }
}
