package net.lab1024.sa.common.security.encrypt;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM field-level encryption service
 *
 * <p>Thread-safe: each call generates a unique 12-byte IV via SecureRandom.
 *
 * <p>Storage format: {@code v1:{ivBase64}:{ciphertextWithAuthTagBase64}}
 *
 * @author iGaming Team
 * @since 2026-02-14
 * @see <a href="../../../architecture/12_Security/02_Encryption_Strategy.md">Encryption
 *     Strategy</a>
 */
@Slf4j
public class AesGcmFieldEncryptService {

  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String CURRENT_VERSION = "v1";

  private final SecretKey dataEncryptionKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public AesGcmFieldEncryptService(SecretKey dataEncryptionKey) {
    this.dataEncryptionKey = dataEncryptionKey;
  }

  /**
   * Encrypt a plaintext value
   *
   * @param plaintext the value to encrypt
   * @return encrypted string in format {@code v1:{iv}:{ciphertext}}, or null if input is null/blank
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      return plaintext;
    }

    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, dataEncryptionKey, spec);

      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      String ivBase64 = Base64.getEncoder().encodeToString(iv);
      String ctBase64 = Base64.getEncoder().encodeToString(ciphertext);

      return CURRENT_VERSION + ":" + ivBase64 + ":" + ctBase64;
    } catch (GeneralSecurityException e) {
      throw new FieldEncryptException("Failed to encrypt field value", e);
    }
  }

  /**
   * Decrypt an encrypted value
   *
   * @param encryptedValue the encrypted string in format {@code v1:{iv}:{ciphertext}}
   * @return decrypted plaintext, or the original value if null/blank or not encrypted
   */
  public String decrypt(String encryptedValue) {
    if (encryptedValue == null || encryptedValue.isBlank()) {
      return encryptedValue;
    }

    if (!isEncrypted(encryptedValue)) {
      return encryptedValue;
    }

    try {
      String[] parts = encryptedValue.split(":", 3);
      if (parts.length < 3) {
        throw new FieldEncryptException(
            "Corrupted encrypted value: expected format 'v1:{iv}:{ciphertext}', got "
                + parts.length
                + " parts",
            null);
      }
      byte[] iv = Base64.getDecoder().decode(parts[1]);
      byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, dataEncryptionKey, spec);

      byte[] plaintext = cipher.doFinal(ciphertext);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new FieldEncryptException("Failed to decrypt field value", e);
    }
  }

  /**
   * Check if a value is encrypted (starts with version prefix)
   *
   * @param value the value to check
   * @return true if the value appears to be encrypted
   */
  public boolean isEncrypted(String value) {
    return value != null && value.startsWith(CURRENT_VERSION + ":");
  }

  /** Runtime exception for field encryption failures */
  public static class FieldEncryptException extends RuntimeException {
    public FieldEncryptException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
