package org.invenio.keycloak.magiclink;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

/**
 * Magic Link Action Token for passwordless authentication
 */
public class MagicLinkActionToken extends DefaultActionToken {

  private static final Logger logger = Logger.getLogger(MagicLinkActionToken.class);

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
      Boolean rememberMe,
      String compoundAuthenticationSessionId) {
    super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null, compoundAuthenticationSessionId);
    this.redirectUri = redirectUri;
    this.issuedFor = clientId;
    this.rememberMe = rememberMe;
    logger.infof(
        "Magic Link Token: Created token - userId=%s, clientId=%s, expiration=%d, authSessionId=%s, nonce=%s, id=%s",
        userId, clientId, absoluteExpirationInSecs, compoundAuthenticationSessionId,
        this.getActionVerificationNonce() != null ? this.getActionVerificationNonce().toString() : "null",
        this.getId());
  }

  // Required for Jackson deserialization
  public MagicLinkActionToken() {
    super();
    logger.info("Magic Link Token: No-arg constructor called (Jackson deserialization)");
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    logger.infof("Magic Link Token: setRedirectUri called - value=%s", redirectUri);
    this.redirectUri = redirectUri;
  }

  public Boolean getRememberMe() {
    return rememberMe;
  }

  public void setRememberMe(Boolean rememberMe) {
    logger.infof("Magic Link Token: setRememberMe called - value=%s", rememberMe);
    this.rememberMe = rememberMe;
    // Log complete state after all setters have been called
    logger.infof("Magic Link Token: After deserialization - %s", this.toString());
  }

  @Override
  public String toString() {
    return String.format(
        "MagicLinkActionToken[id=%s, userId=%s, type=%s, nonce=%s, exp=%d, iat=%d, issuedFor=%s, authSessionId=%s, redirectUri=%s, rememberMe=%s]",
        getId(), getUserId(), getType(), getActionVerificationNonce(),
        getExp() != null ? getExp() : -1, getIat() != null ? getIat() : -1,
        getIssuedFor(), getCompoundAuthenticationSessionId(), redirectUri, rememberMe);
  }
}
