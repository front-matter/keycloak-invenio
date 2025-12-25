package org.frontmatter.keycloak.username;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UsernameGeneratorTest {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^usr_[0-9a-hj-kmnp-z]{8}$");

  @Test
  void testGenerateReturnsNonNull() {
    String username = UsernameGenerator.generate();
    assertNotNull(username, "Generated username should not be null");
  }

  @Test
  void testGenerateHasCorrectFormat() {
    String username = UsernameGenerator.generate();
    assertTrue(USERNAME_PATTERN.matcher(username).matches(),
        "Username should match pattern usr_XXXXXXXX with base32 characters");
  }

  @Test
  void testGenerateStartsWithPrefix() {
    String username = UsernameGenerator.generate();
    assertTrue(username.startsWith("usr_"),
        "Username should start with 'usr_' prefix");
  }

  @Test
  void testGenerateHasCorrectLength() {
    String username = UsernameGenerator.generate();
    assertEquals(12, username.length(),
        "Username should have total length of 12 (usr_ + 8 characters)");
  }

  @Test
  void testGenerateUsesBase32Characters() {
    String username = UsernameGenerator.generate();
    String idPart = username.substring(4); // Remove "usr_" prefix

    for (char c : idPart.toCharArray()) {
      assertTrue(isValidBase32Char(c),
          "Character '" + c + "' should be a valid base32 character");
    }
  }

  @Test
  void testGenerateExcludesAmbiguousCharacters() {
    String username = UsernameGenerator.generate();
    String idPart = username.substring(4);

    // Should not contain i, l, o, u
    assertFalse(idPart.contains("i"), "Should not contain 'i'");
    assertFalse(idPart.contains("l"), "Should not contain 'l'");
    assertFalse(idPart.contains("o"), "Should not contain 'o'");
    assertFalse(idPart.contains("u"), "Should not contain 'u'");
  }

  @RepeatedTest(100)
  void testGenerateProducesRandomValues() {
    String username1 = UsernameGenerator.generate();
    String username2 = UsernameGenerator.generate();

    // While theoretically they could be the same, with 32^8 possibilities it's
    // extremely unlikely
    assertNotEquals(username1, username2,
        "Two consecutive generations should produce different usernames");
  }

  @Test
  void testGenerateUniqueness() {
    Set<String> usernames = new HashSet<>();
    int iterations = 1000;

    for (int i = 0; i < iterations; i++) {
      String username = UsernameGenerator.generate();
      assertTrue(usernames.add(username),
          "All generated usernames should be unique");
    }

    assertEquals(iterations, usernames.size(),
        "Should generate " + iterations + " unique usernames");
  }

  @Test
  void testGenerateIsLowercase() {
    String username = UsernameGenerator.generate();
    assertEquals(username, username.toLowerCase(),
        "Username should be all lowercase");
  }

  @Test
  void testGenerateNoSpecialCharacters() {
    String username = UsernameGenerator.generate();
    String idPart = username.substring(4);

    assertTrue(idPart.matches("[0-9a-z]+"),
        "ID part should only contain alphanumeric characters");
  }

  private boolean isValidBase32Char(char c) {
    return (c >= '0' && c <= '9') ||
        (c >= 'a' && c <= 'h') ||
        (c >= 'j' && c <= 'k') ||
        (c >= 'm' && c <= 'n') ||
        (c >= 'p' && c <= 'z');
  }
}
