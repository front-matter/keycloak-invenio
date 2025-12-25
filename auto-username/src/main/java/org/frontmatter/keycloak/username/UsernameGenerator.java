package org.frontmatter.keycloak.username;

import java.security.SecureRandom;

public final class UsernameGenerator {

  private static final char[] BASE32 = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k',
      'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'
  };

  private static final SecureRandom RANDOM = new SecureRandom();

  public static String generate() {
    char[] buf = new char[8];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = BASE32[RANDOM.nextInt(BASE32.length)];
    }
    return "usr_" + new String(buf);
  }

  private UsernameGenerator() {
  }
}
