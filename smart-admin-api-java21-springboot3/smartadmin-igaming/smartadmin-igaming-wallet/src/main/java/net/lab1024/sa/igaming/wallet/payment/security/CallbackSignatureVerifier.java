package net.lab1024.sa.igaming.wallet.payment.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 callback signature verifier.
 *
 * <p>Uses constant-time comparison to prevent timing attacks. Validates timestamp to prevent replay
 * attacks (5-minute tolerance).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
public class CallbackSignatureVerifier {

  private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L;

  /**
   * Verify callback signature with replay attack prevention.
   *
   * @param secret webhook secret for HMAC computation
   * @param payload raw HTTP body
   * @param signature X-PSP-Signature header value
   * @param timestampMs callback timestamp in milliseconds
   * @return true if signature valid and timestamp within tolerance
   */
  public boolean verify(String secret, String payload, String signature, long timestampMs) {
    // Step 1: Replay attack prevention — check timestamp freshness
    long now = System.currentTimeMillis();
    if (Math.abs(now - timestampMs) > TIMESTAMP_TOLERANCE_MS) {
      log.warn(
          "[Signature] Timestamp expired: delta={}ms, tolerance={}ms",
          Math.abs(now - timestampMs),
          TIMESTAMP_TOLERANCE_MS);
      return false;
    }

    // Step 2: Compute expected HMAC-SHA256
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(keySpec);

      byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String computed = bytesToHex(hmacBytes);

      // Step 3: Constant-time comparison (prevent timing attack)
      boolean valid =
          MessageDigest.isEqual(
              computed.getBytes(StandardCharsets.UTF_8),
              signature.getBytes(StandardCharsets.UTF_8));

      if (!valid) {
        log.error("[Signature] HMAC mismatch");
      }
      return valid;

    } catch (Exception e) {
      log.error("[Signature] Verification error", e);
      return false;
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
