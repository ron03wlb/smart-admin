package net.lab1024.sa.system.mfa.service;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.mfa.manager.MfaTrustedDeviceManager;
import org.springframework.stereotype.Service;

/**
 * MFA Trusted Device Service (Delegation Layer)
 *
 * <p>This service acts as a delegation layer for MFA trusted device operations, forwarding all
 * requests to the underlying MfaTrustedDeviceManager. This design maintains backward compatibility
 * with existing code while ensuring proper layered architecture (Service → Manager → Dao).
 *
 * <p>Business logic, transaction management, and security features are implemented in
 * MfaTrustedDeviceManager.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaTrustedDeviceService {

  private final MfaTrustedDeviceManager mfaTrustedDeviceManager;

  /**
   * Generate device fingerprint from IP + User-Agent.
   *
   * <p>Delegates to MfaTrustedDeviceManager for implementation.
   *
   * @param ipAddress IP address
   * @param userAgent User-Agent string
   * @return SHA256 fingerprint (64-character hex string)
   */
  public String generateDeviceFingerprint(String ipAddress, String userAgent) {
    return mfaTrustedDeviceManager.generateDeviceFingerprint(ipAddress, userAgent);
  }

  /**
   * Check if device is trusted (valid and not expired).
   *
   * <p>Delegates to MfaTrustedDeviceManager for implementation.
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @return true if device is trusted, false otherwise
   */
  public Try<Boolean> isTrustedDevice(Long employeeId, String deviceFingerprint) {
    return mfaTrustedDeviceManager.isTrustedDevice(employeeId, deviceFingerprint);
  }

  /**
   * Add a new trusted device.
   *
   * <p>Delegates to MfaTrustedDeviceManager for implementation.
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @param deviceName Device name (user-provided, e.g., "My iPhone 15")
   * @param ipAddress IP address
   * @param userAgent User-Agent string
   * @return void
   */
  public Try<Void> addTrustedDevice(
      Long employeeId,
      String deviceFingerprint,
      String deviceName,
      String ipAddress,
      String userAgent) {
    return mfaTrustedDeviceManager.addTrustedDevice(
        employeeId, deviceFingerprint, deviceName, ipAddress, userAgent);
  }

  /**
   * Remove a trusted device (soft delete).
   *
   * <p>Delegates to MfaTrustedDeviceManager for implementation.
   *
   * @param deviceId Device ID
   * @return void
   */
  public Try<Void> removeTrustedDevice(Long deviceId) {
    return mfaTrustedDeviceManager.removeTrustedDevice(deviceId);
  }
}
