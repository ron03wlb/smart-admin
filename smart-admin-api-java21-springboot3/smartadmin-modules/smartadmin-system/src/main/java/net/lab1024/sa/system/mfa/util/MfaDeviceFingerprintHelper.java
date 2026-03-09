package net.lab1024.sa.system.mfa.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MFA Device Fingerprint Helper
 *
 * <p>Utility class for generating device fingerprints used in trusted device management.
 *
 * <p>Device fingerprint is a SHA-256 hash of (IP address + User-Agent), providing a consistent
 * identifier for the same device across sessions.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-09
 */
public final class MfaDeviceFingerprintHelper {

  private MfaDeviceFingerprintHelper() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Generate device fingerprint from IP address and User-Agent.
   *
   * <p>Fingerprint = SHA-256(IP + User-Agent)
   *
   * @param ipAddress Client IP address
   * @param userAgent Client User-Agent string
   * @return 64-character hex fingerprint (SHA-256 hash)
   */
  public static String generateFingerprint(String ipAddress, String userAgent) {
    if (ipAddress == null || userAgent == null) {
      throw new IllegalArgumentException("IP address and User-Agent cannot be null");
    }

    try {
      String combined = ipAddress + userAgent;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
