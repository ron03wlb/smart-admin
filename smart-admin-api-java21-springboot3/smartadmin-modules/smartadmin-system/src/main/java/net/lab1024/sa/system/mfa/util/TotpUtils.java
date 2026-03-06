package net.lab1024.sa.system.mfa.util;

import io.vavr.control.Try;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * TOTP (Time-based One-Time Password) Utility Class
 *
 * <p>Pure computational methods for TOTP algorithm (RFC 6238). No Spring dependencies, can be used
 * as static utility methods.
 *
 * <p>Algorithm Details:
 *
 * <ul>
 *   <li>Algorithm: HMAC-SHA1
 *   <li>Time Step: 30 seconds
 *   <li>Token Length: 6 digits
 *   <li>Time Window Tolerance: ±1 step (90 seconds total)
 * </ul>
 *
 * <p>Security:
 *
 * <ul>
 *   <li>TOTP secret is 160-bit (20 bytes) random value
 *   <li>Base32 encoding per RFC 4648
 *   <li>QR code format: otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Slf4j
public final class TotpUtils {

  private static final String ISSUER = "SmartAdmin";
  private static final int TIME_STEP_SECONDS = 30;
  private static final int TOKEN_LENGTH = 6;
  private static final int TIME_WINDOW_TOLERANCE = 1;

  /** Private constructor to prevent instantiation. */
  private TotpUtils() {
    throw new UnsupportedOperationException("Utility class - do not instantiate");
  }

  /**
   * Generate a random TOTP secret (160-bit / 20 bytes).
   *
   * <p>Returns Base32-encoded secret suitable for QR code generation.
   *
   * @return TOTP secret (Base32-encoded)
   */
  public static Try<String> generateTotpSecret() {
    return Try.of(
        () -> {
          SecureRandom random = new SecureRandom();
          byte[] secretBytes = new byte[20]; // 160 bits
          random.nextBytes(secretBytes);
          // Use Base32 encoding (standard for TOTP)
          return base32Encode(secretBytes);
        });
  }

  /**
   * Verify TOTP token against secret.
   *
   * <p>Supports ±1 time window tolerance (90 seconds total).
   *
   * @param secret TOTP secret (Base32-encoded)
   * @param token 6-digit TOTP token entered by user
   * @return true if token is valid, false otherwise
   */
  public static Try<Boolean> verifyTotpToken(String secret, String token) {
    return Try.of(
        () -> {
          if (token == null || token.length() != TOKEN_LENGTH) {
            return false;
          }

          long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

          // Check current time window and ±1 adjacent windows (total 3 windows)
          for (int i = -TIME_WINDOW_TOLERANCE; i <= TIME_WINDOW_TOLERANCE; i++) {
            long timeStep = currentTimeStep + i;
            String expectedToken = generateTotpToken(secret, timeStep);
            if (token.equals(expectedToken)) {
              log.debug("TOTP token verified successfully (time offset: {} windows)", i);
              return true;
            }
          }

          log.warn("TOTP token verification failed: invalid token");
          return false;
        });
  }

  /**
   * Generate QR code URL for Google Authenticator.
   *
   * <p>Format: otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}
   *
   * @param username Employee username (or email)
   * @param secret TOTP secret (Base32-encoded)
   * @return QR code URL
   */
  public static String generateQrCodeUrl(String username, String secret) {
    return String.format(
        "otpauth://totp/%s:%s?secret=%s&issuer=%s", ISSUER, username, secret, ISSUER);
  }

  /**
   * Generate TOTP token for the given secret and time step.
   *
   * @param secret TOTP secret (Base32-encoded)
   * @param timeStep Time step (Unix timestamp / 30)
   * @return 6-digit TOTP token
   */
  private static String generateTotpToken(String secret, long timeStep) throws Exception {
    byte[] secretBytes = base32Decode(secret);
    byte[] message = longToBytes(timeStep);

    // HMAC-SHA1
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
    javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(secretBytes, "RAW");
    mac.init(spec);
    byte[] hash = mac.doFinal(message);

    // Dynamic truncation (RFC 4226)
    int offset = hash[hash.length - 1] & 0x0F;
    int binary =
        ((hash[offset] & 0x7F) << 24)
            | ((hash[offset + 1] & 0xFF) << 16)
            | ((hash[offset + 2] & 0xFF) << 8)
            | (hash[offset + 3] & 0xFF);

    int otp = binary % (int) Math.pow(10, TOKEN_LENGTH);
    return String.format("%0" + TOKEN_LENGTH + "d", otp);
  }

  /**
   * Convert long to byte array (big-endian).
   *
   * @param value Long value to convert
   * @return Byte array (8 bytes)
   */
  private static byte[] longToBytes(long value) {
    byte[] result = new byte[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (byte) (value & 0xFF);
      value >>= 8;
    }
    return result;
  }

  /**
   * Base32 encoding (RFC 4648).
   *
   * <p>Standard Base32 alphabet: A-Z, 2-7 (32 characters)
   *
   * @param data Byte array to encode
   * @return Base32-encoded string
   */
  private static String base32Encode(byte[] data) {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    StringBuilder result = new StringBuilder();
    int buffer = 0;
    int bitsLeft = 0;

    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        result.append(alphabet.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
        bitsLeft -= 5;
      }
    }

    if (bitsLeft > 0) {
      result.append(alphabet.charAt((buffer << (5 - bitsLeft)) & 0x1F));
    }

    // Padding (optional for TOTP, but included for standard compliance)
    while (result.length() % 8 != 0) {
      result.append('=');
    }

    return result.toString();
  }

  /**
   * Base32 decoding (RFC 4648).
   *
   * @param encoded Base32-encoded string
   * @return Decoded byte array
   */
  private static byte[] base32Decode(String encoded) {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    encoded = encoded.toUpperCase().replaceAll("=", "");

    int buffer = 0;
    int bitsLeft = 0;
    java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();

    for (char c : encoded.toCharArray()) {
      int value = alphabet.indexOf(c);
      if (value == -1) {
        throw new IllegalArgumentException("Invalid Base32 character: " + c);
      }

      buffer = (buffer << 5) | value;
      bitsLeft += 5;

      if (bitsLeft >= 8) {
        result.write((buffer >> (bitsLeft - 8)) & 0xFF);
        bitsLeft -= 8;
      }
    }

    return result.toByteArray();
  }
}
