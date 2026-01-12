package org.invenio.keycloak.magiclink;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

/**
 * Magic Link Action Token for passwordless authentication
 */
public class MagicLinkActionToken extends DefaultActionToken {

  public static final String TOKEN_TYPE = "magic-link";

  private static final String JSON_FIELD_REDIRECT_URI = "rdu";
  private static final String JSON_FIELD_REMEMBER_ME = "rme";

  @JsonProperty(value = JSON_FIELD_REDIRECT_URI)
  private String redirectUri;

  @JsonProperty(value = JSON_FIELD_REMEMBER_ME)
  private Boolean rememberMe = false;

  public MagicLinkActionToken(
      String userId,
      int absoluteExpirationInSecs,
      String clientId,
      String redirectUri,
      Boolean rememberMe) {
    super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
    this.redirectUri = redirectUri;
    this.issuedFor = clientId;
    this.rememberMe = rememberMe;
  }

  // Required for Jackson deserialization
  private MagicLinkActionToken() {
    super();
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public Boolean getRememberMe() {
    return rememberMe;
  }

  public void setRememberMe(Boolean rememberMe) {
    this.rememberMe = rememberMe;
  }
}
