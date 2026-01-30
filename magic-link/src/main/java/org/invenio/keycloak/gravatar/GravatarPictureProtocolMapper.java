package org.invenio.keycloak.gravatar;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * Adds a Gravatar URL computed from the user's email as an OIDC token claim.
 */
public final class GravatarPictureProtocolMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

  public static final String PROVIDER_ID = "oidc-gravatar-picture-mapper";

  private static final String CONFIG_GRAVATAR_SIZE = "gravatar.size";
  private static final String CONFIG_GRAVATAR_DEFAULT = "gravatar.default";
  private static final String CONFIG_GRAVATAR_RATING = "gravatar.rating";

  private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

  static {
    ProviderConfigProperty property;

    property = new ProviderConfigProperty();
    property.setName(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
    property.setLabel(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_LABEL);
    property.setType(ProviderConfigProperty.STRING_TYPE);
    property.setHelpText(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_TOOLTIP);
    property.setRequired(true);
    property.setDefaultValue("picture");
    configProperties.add(property);

    property = new ProviderConfigProperty();
    property.setName(OIDCAttributeMapperHelper.JSON_TYPE);
    property.setLabel(OIDCAttributeMapperHelper.JSON_TYPE);
    property.setType(ProviderConfigProperty.LIST_TYPE);
    property.setOptions(List.of("String"));
    property.setHelpText(OIDCAttributeMapperHelper.JSON_TYPE_TOOLTIP);
    property.setDefaultValue("String");
    configProperties.add(property);

    OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, GravatarPictureProtocolMapper.class);

    property = new ProviderConfigProperty();
    property.setName(CONFIG_GRAVATAR_SIZE);
    property.setLabel("Gravatar size");
    property.setHelpText("Image size in pixels (Gravatar 's' query parameter). Default: 200");
    property.setType(ProviderConfigProperty.STRING_TYPE);
    property.setDefaultValue("200");
    configProperties.add(property);

    property = new ProviderConfigProperty();
    property.setName(CONFIG_GRAVATAR_DEFAULT);
    property.setLabel("Gravatar default image");
    property.setHelpText(
        "Fallback if the user has no Gravatar (Gravatar 'd' query parameter). Example: mp, identicon, retro. Default: mp");
    property.setType(ProviderConfigProperty.STRING_TYPE);
    property.setDefaultValue("mp");
    configProperties.add(property);

    property = new ProviderConfigProperty();
    property.setName(CONFIG_GRAVATAR_RATING);
    property.setLabel("Gravatar rating");
    property.setHelpText("Gravatar rating (Gravatar 'r' query parameter). Example: g, pg, r, x. Default: g");
    property.setType(ProviderConfigProperty.STRING_TYPE);
    property.setDefaultValue("g");
    configProperties.add(property);
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayType() {
    return "Gravatar picture";
  }

  @Override
  public String getDisplayCategory() {
    return TOKEN_MAPPER_CATEGORY;
  }

  @Override
  public String getHelpText() {
    return "Computes a Gravatar URL from the user email and maps it to a token claim (default: picture).";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return configProperties;
  }

  @Override
  protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
    UserModel user = userSession.getUser();
    if (user == null) {
      return;
    }

    String email = user.getEmail();
    String url = gravatarUrlForEmail(email, getSize(mappingModel), getDefaultImage(mappingModel),
        getRating(mappingModel));
    if (url == null) {
      return;
    }

    OIDCAttributeMapperHelper.mapClaim(token, mappingModel, url);
  }

  static String gravatarUrlForEmail(String email, int size, String defaultImage, String rating) {
    if (email == null) {
      return null;
    }

    String normalized = email.trim().toLowerCase();
    if (normalized.isEmpty()) {
      return null;
    }

    String hash = md5Hex(normalized);

    StringBuilder sb = new StringBuilder("https://www.gravatar.com/avatar/");
    sb.append(hash);

    boolean hasQuery = false;

    if (size > 0) {
      sb.append(hasQuery ? "&" : "?");
      hasQuery = true;
      sb.append("s=").append(size);
    }

    if (defaultImage != null && !defaultImage.isBlank()) {
      sb.append(hasQuery ? "&" : "?");
      hasQuery = true;
      sb.append("d=").append(urlEncode(defaultImage.trim()));
    }

    if (rating != null && !rating.isBlank()) {
      sb.append(hasQuery ? "&" : "?");
      hasQuery = true;
      sb.append("r=").append(urlEncode(rating.trim()));
    }

    return sb.toString();
  }

  private static int getSize(ProtocolMapperModel mappingModel) {
    String raw = mappingModel.getConfig().get(CONFIG_GRAVATAR_SIZE);
    if (raw == null || raw.isBlank()) {
      return 200;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return 200;
    }
  }

  private static String getDefaultImage(ProtocolMapperModel mappingModel) {
    String raw = mappingModel.getConfig().get(CONFIG_GRAVATAR_DEFAULT);
    return raw == null ? "mp" : raw;
  }

  private static String getRating(ProtocolMapperModel mappingModel) {
    String raw = mappingModel.getConfig().get(CONFIG_GRAVATAR_RATING);
    return raw == null ? "g" : raw;
  }

  private static String md5Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 algorithm not available", e);
    }
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
