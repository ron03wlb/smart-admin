package net.lab1024.sa.app.security;

import static org.junit.jupiter.api.Assertions.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService.FieldEncryptException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AES-256-GCM field encryption service.
 *
 * @since 2026-02-17
 */
class AesGcmFieldEncryptServiceTest {

  private static AesGcmFieldEncryptService service;

  @BeforeAll
  static void setUp() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    SecretKey key = keyGen.generateKey();
    service = new AesGcmFieldEncryptService(key);
  }

  @Test
  void encryptAndDecryptRoundTrip() {
    String plaintext = "sensitive-data-12345";
    String encrypted = service.encrypt(plaintext);

    assertNotNull(encrypted);
    assertNotEquals(plaintext, encrypted);
    assertTrue(encrypted.startsWith("v1:"));

    String decrypted = service.decrypt(encrypted);
    assertEquals(plaintext, decrypted);
  }

  @Test
  void encryptProducesDifferentCiphertextEachTime() {
    String plaintext = "same-input";
    String encrypted1 = service.encrypt(plaintext);
    String encrypted2 = service.encrypt(plaintext);

    assertNotEquals(encrypted1, encrypted2, "Each encryption should use a unique IV");
    assertEquals(plaintext, service.decrypt(encrypted1));
    assertEquals(plaintext, service.decrypt(encrypted2));
  }

  @Test
  void encryptNullReturnsNull() {
    assertNull(service.encrypt(null));
  }

  @Test
  void encryptBlankReturnsBlank() {
    assertEquals("", service.encrypt(""));
    assertEquals("   ", service.encrypt("   "));
  }

  @Test
  void decryptNullReturnsNull() {
    assertNull(service.decrypt(null));
  }

  @Test
  void decryptNonEncryptedValueReturnsAsIs() {
    String plaintext = "not-encrypted-data";
    assertEquals(plaintext, service.decrypt(plaintext));
  }

  @Test
  void decryptCorruptedValueThrowsWithClearMessage() {
    String corrupted = "v1:onlyOnePartAfterPrefix";
    FieldEncryptException ex =
        assertThrows(FieldEncryptException.class, () -> service.decrypt(corrupted));
    assertTrue(ex.getMessage().contains("Corrupted encrypted value"));
  }

  @Test
  void isEncryptedReturnsTrueForEncryptedValues() {
    String encrypted = service.encrypt("test");
    assertTrue(service.isEncrypted(encrypted));
  }

  @Test
  void isEncryptedReturnsFalseForPlaintext() {
    assertFalse(service.isEncrypted("plaintext"));
    assertFalse(service.isEncrypted(null));
    assertFalse(service.isEncrypted(""));
  }

  @Test
  void handlesUnicodeContent() {
    String unicode = "中文測試 日本語テスト 한국어 🎰💰";
    String encrypted = service.encrypt(unicode);
    String decrypted = service.decrypt(encrypted);
    assertEquals(unicode, decrypted);
  }
}
