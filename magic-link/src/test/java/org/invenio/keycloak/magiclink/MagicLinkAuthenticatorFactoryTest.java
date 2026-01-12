package org.invenio.keycloak.magiclink;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticationExecutionModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MagicLinkAuthenticatorFactoryTest {

  private MagicLinkAuthenticatorFactory factory;

  @BeforeEach
  void setUp() {
    factory = new MagicLinkAuthenticatorFactory();
  }

  @Test
  void testGetId() {
    assertEquals("magic-link-authenticator", factory.getId());
  }

  @Test
  void testGetDisplayType() {
    assertEquals("Magic Link", factory.getDisplayType());
  }

  @Test
  void testGetReferenceCategory() {
    assertEquals("passwordless", factory.getReferenceCategory());
  }

  @Test
  void testIsConfigurable() {
    assertTrue(factory.isConfigurable());
  }

  @Test
  void testIsUserSetupAllowed() {
    assertFalse(factory.isUserSetupAllowed());
  }

  @Test
  void testGetRequirementChoices() {
    AuthenticationExecutionModel.Requirement[] requirements = factory.getRequirementChoices();
    assertEquals(3, requirements.length);
    assertTrue(contains(requirements, AuthenticationExecutionModel.Requirement.REQUIRED));
    assertTrue(contains(requirements, AuthenticationExecutionModel.Requirement.ALTERNATIVE));
    assertTrue(contains(requirements, AuthenticationExecutionModel.Requirement.DISABLED));
  }

  @Test
  void testGetHelpText() {
    String helpText = factory.getHelpText();
    assertNotNull(helpText);
    assertTrue(helpText.contains("Passwordless"));
    assertTrue(helpText.contains("magic link"));
  }

  @Test
  void testGetConfigProperties() {
    var properties = factory.getConfigProperties();
    assertEquals(2, properties.size());

    var createUserProp = properties.stream()
        .filter(p -> "createUser".equals(p.getName()))
        .findFirst();
    assertTrue(createUserProp.isPresent());
    assertEquals("Auto-create users", createUserProp.get().getLabel());

    var tokenValidityProp = properties.stream()
        .filter(p -> "tokenValidity".equals(p.getName()))
        .findFirst();
    assertTrue(tokenValidityProp.isPresent());
    assertEquals("Token validity (seconds)", tokenValidityProp.get().getLabel());
  }

  @Test
  void testCreate() {
    var authenticator = factory.create(null);
    assertNotNull(authenticator);
    assertInstanceOf(MagicLinkAuthenticator.class, authenticator);
  }

  @Test
  void testLifecycleMethods() {
    // Should not throw exceptions
    factory.init(null);
    factory.postInit(null);
    factory.close();
  }

  private boolean contains(AuthenticationExecutionModel.Requirement[] array,
      AuthenticationExecutionModel.Requirement value) {
    for (AuthenticationExecutionModel.Requirement r : array) {
      if (r == value)
        return true;
    }
    return false;
  }
}
