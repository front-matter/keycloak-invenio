package org.frontmatter.keycloak.username;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class AutoUsernameAuthenticator implements Authenticator {

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    UserModel user = context.getUser();

    // Only act on newly created users without a username
    if (user != null && user.getUsername() == null) {
      assignUsername(context.getSession(), context.getRealm(), user);
    }

    context.success();
  }

  private void assignUsername(KeycloakSession session, RealmModel realm, UserModel user) {
    for (int attempt = 0; attempt < 3; attempt++) {
      String username = UsernameGenerator.generate();

      if (session.users().getUserByUsername(realm, username) == null) {
        user.setUsername(username);
        return;
      }
    }

    throw new RuntimeException("Failed to generate unique username after retries");
  }

  @Override
  public void action(AuthenticationFlowContext context) {
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
  }

  @Override
  public void close() {
  }
}
