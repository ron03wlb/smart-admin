package net.lab1024.sa.system.mfa.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.mfa.dao.MfaAuditLogDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaAuditLogEntity;
import net.lab1024.sa.system.mfa.service.MfaBackupCodeService;
import net.lab1024.sa.system.mfa.service.MfaService;
import net.lab1024.sa.system.mfa.service.MfaTrustedDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA Verification Manager
 *
 * <p>Provides transactional methods for MFA verification operations. All methods in this class are
 * wrapped in database transactions to ensure data consistency.
 *
 * <p>Key Operations:
 *
 * <ul>
 *   <li>Verify TOTP token
 *   <li>Verify backup code
 *   <li>Add trusted device
 *   <li>Audit logging for all verification attempts
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaVerificationManager {

  private final MfaService mfaService;
  private final MfaBackupCodeService mfaBackupCodeService;
  private final MfaTrustedDeviceService mfaTrustedDeviceService;
  private final MfaAuditLogDao mfaAuditLogDao;

  /**
   * Verify TOTP token and log audit (transaction method).
   *
   * @param employeeId Employee ID
   * @param secret TOTP secret (Base32-encoded)
   * @param token 6-digit TOTP token
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @return true if token is valid, false otherwise
   */
  @Transactional(rollbackFor = Throwable.class)
  public boolean verifyTotpTokenTransaction(
      Long employeeId, String secret, String token, String ipAddress, String userAgent) {

    boolean verified =
        mfaService
            .verifyTotpToken(secret, token)
            .getOrElse(
                () -> {
                  log.error("Failed to verify TOTP token for employee ID: {}", employeeId);
                  return false;
                });

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_VERIFY_TOTP");
    auditLog.setEventResult(verified ? "SUCCESS" : "FAILURE");
    auditLog.setSeverity(verified ? "INFO" : "WARNING");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    if (!verified) {
      auditLog.setErrorMessage("Invalid TOTP token");
    }
    mfaAuditLogDao.insert(auditLog);

    if (verified) {
      log.info(
          "TOTP token verified successfully for employee ID: {} from IP: {}",
          employeeId,
          ipAddress);
    } else {
      log.warn(
          "TOTP token verification failed for employee ID: {} from IP: {}", employeeId, ipAddress);
    }

    return verified;
  }

  /**
   * Verify backup code and log audit (transaction method).
   *
   * @param employeeId Employee ID
   * @param backupCode Plaintext backup code
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @return true if code is valid and unused, false otherwise
   */
  @Transactional(rollbackFor = Throwable.class)
  public boolean verifyBackupCodeTransaction(
      Long employeeId, String backupCode, String ipAddress, String userAgent) {

    boolean verified =
        mfaBackupCodeService
            .verifyBackupCode(employeeId, backupCode, ipAddress)
            .getOrElse(
                () -> {
                  log.error("Failed to verify backup code for employee ID: {}", employeeId);
                  return false;
                });

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("BACKUP_CODE_USED");
    auditLog.setEventResult(verified ? "SUCCESS" : "FAILURE");
    auditLog.setSeverity(verified ? "WARNING" : "WARNING"); // Backup code usage is always WARNING
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    if (!verified) {
      auditLog.setErrorMessage("Invalid or already used backup code");
    }
    mfaAuditLogDao.insert(auditLog);

    if (verified) {
      log.info(
          "Backup code verified successfully for employee ID: {} from IP: {}",
          employeeId,
          ipAddress);
    } else {
      log.warn(
          "Backup code verification failed for employee ID: {} from IP: {}", employeeId, ipAddress);
    }

    return verified;
  }

  /**
   * Add trusted device and log audit (transaction method).
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @param deviceName Device name (user-provided)
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   */
  @Transactional(rollbackFor = Throwable.class)
  public void addTrustedDeviceTransaction(
      Long employeeId,
      String deviceFingerprint,
      String deviceName,
      String ipAddress,
      String userAgent) {

    mfaTrustedDeviceService
        .addTrustedDevice(employeeId, deviceFingerprint, deviceName, ipAddress, userAgent)
        .onFailure(
            e -> log.error("Failed to add trusted device for employee ID: {}", employeeId, e));

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("TRUSTED_DEVICE_ADDED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("INFO");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info(
        "Trusted device added for employee ID: {} (name: {}) from IP: {}",
        employeeId,
        deviceName,
        ipAddress);
  }

  /**
   * Record MFA verification failure and check for account lockout (transaction method).
   *
   * @param employeeId Employee ID
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @param errorMessage Error message
   * @return Number of recent failures (for lockout detection)
   */
  @Transactional(rollbackFor = Throwable.class)
  public int recordVerificationFailureTransaction(
      Long employeeId, String ipAddress, String userAgent, String errorMessage) {

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_VERIFY_FAIL");
    auditLog.setEventResult("FAILURE");
    auditLog.setSeverity("WARNING");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setErrorMessage(errorMessage);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    // Count recent failures (last 15 minutes)
    Integer failureCount = mfaAuditLogDao.countRecentFailures(employeeId, 15);
    if (failureCount != null && failureCount >= 3) {
      log.warn(
          "Multiple MFA verification failures detected for employee ID: {} ({} failures in 15 minutes)",
          employeeId,
          failureCount);
    }

    return failureCount != null ? failureCount : 0;
  }
}
