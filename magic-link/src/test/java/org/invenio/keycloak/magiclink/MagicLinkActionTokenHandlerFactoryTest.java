package org.invenio.keycloak.magiclink;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagicLinkActionTokenHandlerFactoryTest {

  private MagicLinkActionTokenHandlerFactory factory;

  @BeforeEach
  void setUp() {
    factory = new MagicLinkActionTokenHandlerFactory();
  }

  @Test
  void testGetId() {
    assertEquals("magic-link-token-handler", factory.getId());
  }

  @Test
  void testCreate() {
    var handler = factory.create(null);
    assertNotNull(handler);
    assertInstanceOf(MagicLinkActionTokenHandler.class, handler);
  }

  @Test
  void testLifecycleMethods() {
    // Should not throw exceptions
    factory.init(null);
    factory.postInit(null);
    factory.close();
  }
}
