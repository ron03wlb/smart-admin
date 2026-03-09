package net.lab1024.sa.system.mfa.manager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.mfa.dao.MfaAuditLogDao;
import net.lab1024.sa.system.mfa.dao.MfaConfigDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaAuditLogEntity;
import net.lab1024.sa.system.mfa.domain.entity.MfaConfigEntity;
import net.lab1024.sa.system.mfa.service.MfaBackupCodeService;
import net.lab1024.sa.system.mfa.service.MfaTrustedDeviceService;
import net.lab1024.sa.system.mfa.util.TotpUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA Verification Manager
 *
 * <p>Provides transactional methods for MFA verification operations.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaVerificationManager {

  private final MfaConfigDao mfaConfigDao;
  private final MfaAuditLogDao mfaAuditLogDao;
  private final MfaBackupCodeService mfaBackupCodeService;
  private final MfaTrustedDeviceService mfaTrustedDeviceService;

  /**
   * Verify MFA with optional trusted device creation (transaction method).
   *
   * <p>Atomic operation includes:
   *
   * <ul>
   *   <li>Try TOTP verification (6 digits)
   *   <li>Try backup code verification (8 digits) if TOTP fails
   *   <li>Update last verified timestamp
   *   <li>Add trusted device (if requested)
   *   <li>Record audit logs for all operations
   * </ul>
   *
   * @param employeeId Employee ID
   * @param secret Decrypted TOTP secret
   * @param mfaToken MFA token (6-digit TOTP or 8-digit backup code)
   * @param ipAddress IP address
   * @param userAgent User-Agent
   * @param trustDevice Whether to add trusted device
   * @param deviceName Device name (required if trustDevice = true)
   * @return Success response or error
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> verifyMfaWithTrustedDeviceTransaction(
      Long employeeId,
      String secret,
      String mfaToken,
      String ipAddress,
      String userAgent,
      Boolean trustDevice,
      String deviceName) {

    boolean verified = false;
    String verificationMethod = null;

    // Try TOTP verification (6 digits)
    if (mfaToken.matches("^[0-9]{6}$")) {
      verified = TotpUtils.verifyTotpToken(secret, mfaToken).getOrElse(false);
      verificationMethod = "TOTP";

      if (verified) {
        recordAuditLog(
            employeeId, "MFA_VERIFY_TOTP", "SUCCESS", "INFO", ipAddress, userAgent, null);
      } else {
        recordAuditLog(
            employeeId,
            "MFA_VERIFY_TOTP",
            "FAILURE",
            "WARNING",
            ipAddress,
            userAgent,
            "Invalid TOTP token");
      }
    }

    // Try backup code verification (8 digits)
    if (!verified && mfaToken.matches("^[0-9]{8}$")) {
      verified =
          mfaBackupCodeService.verifyBackupCode(employeeId, mfaToken, ipAddress).getOrElse(false);
      verificationMethod = "BACKUP_CODE";

      if (verified) {
        recordAuditLog(
            employeeId, "BACKUP_CODE_USED", "SUCCESS", "INFO", ipAddress, userAgent, null);
      } else {
        recordAuditLog(
            employeeId,
            "BACKUP_CODE_USED",
            "FAILURE",
            "WARNING",
            ipAddress,
            userAgent,
            "Invalid backup code");
      }
    }

    if (!verified) {
      return ResponseDTO.userErrorParam("MFA 驗證碼錯誤");
    }

    // Update last verified timestamp
    MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
    if (config != null) {
      config.setLastVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
      mfaConfigDao.updateById(config);
    }

    // Add trusted device if requested
    if (Boolean.TRUE.equals(trustDevice)) {
      String fingerprint = mfaTrustedDeviceService.generateDeviceFingerprint(ipAddress, userAgent);
      mfaTrustedDeviceService
          .addTrustedDevice(employeeId, fingerprint, deviceName, ipAddress, userAgent)
          .getOrElseThrow(e -> new RuntimeException("Failed to add trusted device", e));

      recordAuditLog(
          employeeId, "TRUSTED_DEVICE_ADDED", "SUCCESS", "INFO", ipAddress, userAgent, null);
    }

    log.info(
        "MFA verification successful: employeeId={}, method={}, trustDevice={}",
        employeeId,
        verificationMethod,
        trustDevice);

    return ResponseDTO.ok("MFA 驗證成功");
  }

  /**
   * Record audit log (internal helper method).
   *
   * @param employeeId Employee ID
   * @param eventType Event type
   * @param eventResult Event result (SUCCESS, FAILURE)
   * @param severity Severity (INFO, WARNING, CRITICAL)
   * @param ipAddress IP address
   * @param userAgent User-Agent
   * @param errorMessage Error message (nullable)
   */
  private void recordAuditLog(
      Long employeeId,
      String eventType,
      String eventResult,
      String severity,
      String ipAddress,
      String userAgent,
      String errorMessage) {

    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType(eventType);
    auditLog.setEventResult(eventResult);
    auditLog.setSeverity(severity);
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    if (errorMessage != null) {
      auditLog.setErrorMessage(errorMessage);
    }
    mfaAuditLogDao.insert(auditLog);
  }
}
