package net.lab1024.sa.common.security.encrypt;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

/**
 * HMAC-SHA256 Blind Index service for searchable encrypted fields
 *
 * <p>Produces a deterministic 64-character lowercase hex string for a given plaintext input. This
 * allows database indexing and equality queries on encrypted PII fields without exposing plaintext.
 *
 * @author iGaming Team
 * @since 2026-02-14
 * @see <a href="../../../architecture/12_Security/03_Blind_Index_Architecture.md">Blind Index
 *     Architecture</a>
 */
@Slf4j
public class BlindIndexService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final SecretKey blindIndexKey;

  public BlindIndexService(SecretKey blindIndexKey) {
    this.blindIndexKey = blindIndexKey;
  }

  /**
   * Compute a blind index for the given plaintext
   *
   * @param plaintext the value to index (e.g. phone number, email)
   * @return 64-character lowercase hex string, or null if input is null/blank
   */
  public String computeIndex(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      return null;
    }

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(blindIndexKey);
      byte[] hash = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new BlindIndexException("Failed to compute blind index", e);
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /** Runtime exception for blind index computation failures */
  public static class BlindIndexException extends RuntimeException {
    public BlindIndexException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
