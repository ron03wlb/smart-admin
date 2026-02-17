package net.lab1024.sa.app.security;

import static org.junit.jupiter.api.Assertions.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HMAC-SHA256 blind index service.
 *
 * @since 2026-02-17
 */
class BlindIndexServiceTest {

  private static BlindIndexService service;

  @BeforeAll
  static void setUp() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
    SecretKey key = keyGen.generateKey();
    service = new BlindIndexService(key);
  }

  @Test
  void computeIndexIsDeterministic() {
    String input = "test@example.com";
    String index1 = service.computeIndex(input);
    String index2 = service.computeIndex(input);

    assertEquals(index1, index2, "Same input must produce same blind index");
  }

  @Test
  void computeIndexProduces64CharHex() {
    String index = service.computeIndex("some-value");

    assertNotNull(index);
    assertEquals(64, index.length());
    assertTrue(index.matches("[0-9a-f]{64}"), "Must be lowercase hex");
  }

  @Test
  void differentInputsProduceDifferentIndexes() {
    String index1 = service.computeIndex("input-a");
    String index2 = service.computeIndex("input-b");

    assertNotEquals(index1, index2);
  }

  @Test
  void nullInputReturnsNull() {
    assertNull(service.computeIndex(null));
  }

  @Test
  void blankInputReturnsNull() {
    assertNull(service.computeIndex(""));
    assertNull(service.computeIndex("   "));
  }

  @Test
  void differentKeysProduceDifferentIndexes() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
    SecretKey otherKey = keyGen.generateKey();
    BlindIndexService otherService = new BlindIndexService(otherKey);

    String input = "same-input";
    String index1 = service.computeIndex(input);
    String index2 = otherService.computeIndex(input);

    assertNotEquals(index1, index2, "Different keys must produce different indexes");
  }
}
