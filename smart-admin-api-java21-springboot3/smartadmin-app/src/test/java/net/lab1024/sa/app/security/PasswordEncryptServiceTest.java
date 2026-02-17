package net.lab1024.sa.app.security;

import static org.junit.jupiter.api.Assertions.*;

import net.lab1024.sa.common.security.config.Argon2Properties;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Argon2id password encryption service.
 *
 * @since 2026-02-17
 */
class PasswordEncryptServiceTest {

  private static PasswordEncryptService service;

  @BeforeAll
  static void setUp() {
    Argon2Properties props = new Argon2Properties();
    // Use default params (m=16384, t=2, p=1) for fast test execution
    service = new PasswordEncryptService(props);
  }

  @Test
  void encryptProducesArgon2idHash() {
    String hash = service.encrypt("password123");

    assertNotNull(hash);
    assertTrue(hash.startsWith("$argon2id$"), "Must use argon2id variant");
  }

  @Test
  void matchesReturnsTrueForCorrectPassword() {
    String raw = "correctPassword!@#";
    String hash = service.encrypt(raw);

    assertTrue(service.matches(raw, hash));
  }

  @Test
  void matchesReturnsFalseForWrongPassword() {
    String hash = service.encrypt("correctPassword");

    assertFalse(service.matches("wrongPassword", hash));
  }

  @Test
  void encryptProducesDifferentHashEachTime() {
    String raw = "samePassword";
    String hash1 = service.encrypt(raw);
    String hash2 = service.encrypt(raw);

    assertNotEquals(hash1, hash2, "Each hash should use a unique salt");
    assertTrue(service.matches(raw, hash1));
    assertTrue(service.matches(raw, hash2));
  }

  @Test
  void needsUpgradeReturnsFalseForCurrentParams() {
    String hash = service.encrypt("password");

    assertFalse(service.needsUpgrade(hash));
  }

  @Test
  void needsUpgradeReturnsTrueForDifferentParams() {
    // Simulate a hash with different memory parameter
    String oldHash = "$argon2id$v=19$m=8192,t=2,p=1$fakeSalt$fakeHash";

    assertTrue(service.needsUpgrade(oldHash));
  }

  @Test
  void needsUpgradeReturnsFalseForNull() {
    assertFalse(service.needsUpgrade(null));
  }

  @Test
  void needsUpgradeReturnsFalseForNonArgon2Hash() {
    assertFalse(service.needsUpgrade("$2a$10$bcryptHash"));
  }
}
