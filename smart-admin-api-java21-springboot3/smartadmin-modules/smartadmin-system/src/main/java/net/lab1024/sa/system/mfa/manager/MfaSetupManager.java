package net.lab1024.sa.system.mfa.manager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.mfa.dao.MfaAuditLogDao;
import net.lab1024.sa.system.mfa.dao.MfaBackupCodeDao;
import net.lab1024.sa.system.mfa.dao.MfaConfigDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaAuditLogEntity;
import net.lab1024.sa.system.mfa.domain.entity.MfaConfigEntity;
import net.lab1024.sa.system.mfa.domain.vo.MfaSetupInitVO;
import net.lab1024.sa.system.mfa.util.TotpUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA Setup Manager
 *
 * <p>Provides transactional methods for MFA setup operations. All methods in this class are wrapped
 * in database transactions to ensure data consistency.
 *
 * <p>Key Operations:
 *
 * <ul>
 *   <li>Enable MFA (create config + generate backup codes)
 *   <li>Disable MFA (soft delete config + backup codes)
 *   <li>Regenerate backup codes
 *   <li>Audit logging for all setup operations
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaSetupManager {

  private final MfaConfigDao mfaConfigDao;
  private final MfaBackupCodeDao mfaBackupCodeDao;
  private final MfaAuditLogDao mfaAuditLogDao;
  private final MfaBackupCodeManager mfaBackupCodeManager;
  private final MfaTrustedDeviceManager mfaTrustedDeviceManager;

  /**
   * Enable MFA for employee (transaction method).
   *
   * <p>Creates MFA configuration with encrypted TOTP secret and generates 10 backup codes.
   *
   * @param employeeId Employee ID
   * @param encryptedSecret TOTP secret (already AES-256-GCM encrypted)
   * @param qrCodeConfirmed Whether QR code was scanned and confirmed
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @return List of 10 plaintext backup codes (only shown once to user)
   */
  @Transactional(rollbackFor = Throwable.class)
  public List<String> enableMfaTransaction(
      Long employeeId,
      String encryptedSecret,
      Boolean qrCodeConfirmed,
      String ipAddress,
      String userAgent) {

    // Create MFA configuration
    MfaConfigEntity config = new MfaConfigEntity();
    config.setEmployeeId(employeeId);
    config.setMfaEnabled(true);
    config.setMfaType("TOTP");
    config.setSecretEncrypted(encryptedSecret);
    config.setBackupCodesGenerated(false); // Will be set to true after backup codes are generated
    config.setQrCodeConfirmed(qrCodeConfirmed);
    config.setEnforcedByRole(false);
    config.setDeleted(false);
    mfaConfigDao.insert(config);

    // Generate backup codes
    List<String> backupCodes =
        mfaBackupCodeManager
            .generateBackupCodes(employeeId)
            .getOrElseThrow(e -> new RuntimeException("Failed to generate backup codes", e));

    // Update config to mark backup codes as generated
    config.setBackupCodesGenerated(true);
    mfaConfigDao.updateById(config);

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_ENABLED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("INFO");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info("MFA enabled successfully for employee ID: {} from IP: {}", employeeId, ipAddress);

    return backupCodes;
  }

  /**
   * Disable MFA for employee (transaction method).
   *
   * <p>Soft deletes MFA configuration and all backup codes.
   *
   * @param employeeId Employee ID
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   */
  @Transactional(rollbackFor = Throwable.class)
  public void disableMfaTransaction(Long employeeId, String ipAddress, String userAgent) {

    // Soft delete MFA configuration
    MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
    if (config != null) {
      config.setDeleted(true);
      mfaConfigDao.updateById(config);
    }

    // Soft delete all backup codes
    mfaBackupCodeDao.deleteByEmployeeId(employeeId);

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_DISABLED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("WARNING"); // Disabling MFA is a security-relevant event
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info("MFA disabled for employee ID: {} from IP: {}", employeeId, ipAddress);
  }

  /**
   * Regenerate backup codes for employee (transaction method).
   *
   * <p>Soft deletes old backup codes and generates new ones.
   *
   * @param employeeId Employee ID
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @return List of 10 new plaintext backup codes
   */
  @Transactional(rollbackFor = Throwable.class)
  public List<String> regenerateBackupCodesTransaction(
      Long employeeId, String ipAddress, String userAgent) {

    // Generate new backup codes (automatically deletes old ones)
    List<String> backupCodes =
        mfaBackupCodeManager
            .generateBackupCodes(employeeId)
            .getOrElseThrow(e -> new RuntimeException("Failed to regenerate backup codes", e));

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("BACKUP_CODES_REGENERATED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("INFO");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info("Backup codes regenerated for employee ID: {} from IP: {}", employeeId, ipAddress);

    return backupCodes;
  }

  /**
   * Update last verified timestamp (transaction method).
   *
   * <p>Called after successful MFA verification to track last verification time.
   *
   * @param employeeId Employee ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateLastVerifiedTransaction(Long employeeId) {
    MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
    if (config != null) {
      config.setLastVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
      mfaConfigDao.updateById(config);
    }
  }

  /**
   * Record MFA verification audit log (transaction method).
   *
   * <p>Used by MfaService to record TOTP/backup code verification attempts.
   *
   * @param employeeId Employee ID
   * @param eventType Event type (e.g., "MFA_VERIFY_TOTP", "BACKUP_CODE_USED")
   * @param success Verification result (true = success, false = failure)
   * @param ipAddress IP address
   * @param userAgent User-Agent
   * @param errorMessage Error message (if failed)
   */
  @Transactional(rollbackFor = Throwable.class)
  public void recordMfaVerificationAuditTransaction(
      Long employeeId,
      String eventType,
      boolean success,
      String ipAddress,
      String userAgent,
      String errorMessage) {

    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType(eventType);
    auditLog.setEventResult(success ? "SUCCESS" : "FAILURE");
    auditLog.setSeverity(success ? "INFO" : "WARNING");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    if (!success && errorMessage != null) {
      auditLog.setErrorMessage(errorMessage);
    }
    mfaAuditLogDao.insert(auditLog);

    log.info(
        "MFA verification audit: employee={}, event={}, result={}",
        employeeId,
        eventType,
        success ? "SUCCESS" : "FAILURE");
  }

  /**
   * Record general MFA audit log (transaction method).
   *
   * <p>Used for recording non-verification MFA events (e.g., trusted device added).
   *
   * @param employeeId Employee ID
   * @param eventType Event type (e.g., "TRUSTED_DEVICE_ADDED")
   * @param eventResult Event result ("SUCCESS", "FAILURE")
   * @param ipAddress IP address
   * @param userAgent User-Agent
   * @param errorMessage Error message (if any)
   */
  @Transactional(rollbackFor = Throwable.class)
  public void recordMfaAuditLogTransaction(
      Long employeeId,
      String eventType,
      String eventResult,
      String ipAddress,
      String userAgent,
      String errorMessage) {

    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType(eventType);
    auditLog.setEventResult(eventResult);
    auditLog.setSeverity("INFO");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    if (errorMessage != null) {
      auditLog.setErrorMessage(errorMessage);
    }
    mfaAuditLogDao.insert(auditLog);

    log.info("MFA audit: employee={}, event={}, result={}", employeeId, eventType, eventResult);
  }

  /**
   * Enable MFA with optional trusted device (transaction method).
   *
   * <p>Atomic operation includes:
   *
   * <ul>
   *   <li>Create MFA configuration
   *   <li>Generate backup codes
   *   <li>Add trusted device (if requested)
   *   <li>Record audit log
   * </ul>
   *
   * @param employeeId Employee ID
   * @param encryptedSecret TOTP secret (already AES-256-GCM encrypted)
   * @param qrCodeConfirmed Whether QR code was scanned and confirmed
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @param trustDevice Whether to add trusted device
   * @param deviceName Device name (required if trustDevice = true)
   * @return List of 10 plaintext backup codes (only shown once to user)
   */
  @Transactional(rollbackFor = Throwable.class)
  public List<String> enableMfaWithTrustedDeviceTransaction(
      Long employeeId,
      String encryptedSecret,
      Boolean qrCodeConfirmed,
      String ipAddress,
      String userAgent,
      Boolean trustDevice,
      String deviceName) {

    // Create MFA configuration
    MfaConfigEntity config = new MfaConfigEntity();
    config.setEmployeeId(employeeId);
    config.setMfaEnabled(true);
    config.setMfaType("TOTP");
    config.setSecretEncrypted(encryptedSecret);
    config.setBackupCodesGenerated(false);
    config.setQrCodeConfirmed(qrCodeConfirmed);
    config.setEnforcedByRole(false);
    config.setDeleted(false);
    mfaConfigDao.insert(config);

    // Generate backup codes
    List<String> backupCodes =
        mfaBackupCodeManager
            .generateBackupCodes(employeeId)
            .getOrElseThrow(e -> new RuntimeException("Failed to generate backup codes", e));

    // Update config to mark backup codes as generated
    config.setBackupCodesGenerated(true);
    mfaConfigDao.updateById(config);

    // Add trusted device if requested
    if (Boolean.TRUE.equals(trustDevice)) {
      String fingerprint = mfaTrustedDeviceManager.generateDeviceFingerprint(ipAddress, userAgent);
      mfaTrustedDeviceManager
          .addTrustedDevice(employeeId, fingerprint, deviceName, ipAddress, userAgent)
          .getOrElseThrow(e -> new RuntimeException("Failed to add trusted device", e));
    }

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_ENABLED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("INFO");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info("MFA enabled successfully for employee ID: {} from IP: {}", employeeId, ipAddress);

    return backupCodes;
  }

  /**
   * Disable MFA with TOTP verification (transaction method).
   *
   * <p>Atomic operation includes:
   *
   * <ul>
   *   <li>Verify TOTP token
   *   <li>Soft delete MFA configuration
   *   <li>Soft delete all backup codes
   *   <li>Record audit log
   * </ul>
   *
   * @param employeeId Employee ID
   * @param secret Decrypted TOTP secret
   * @param totpToken TOTP token for verification
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @return Success response or error
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> disableMfaWithVerificationTransaction(
      Long employeeId, String secret, String totpToken, String ipAddress, String userAgent) {

    // Verify TOTP
    boolean verified =
        TotpUtils.verifyTotpToken(secret, totpToken)
            .getOrElseThrow(e -> new RuntimeException("Failed to verify TOTP token", e));

    if (!verified) {
      // Record failed verification audit
      recordMfaVerificationAuditTransaction(
          employeeId, "MFA_DISABLE_ATTEMPT", false, ipAddress, userAgent, "Invalid TOTP token");
      return ResponseDTO.userErrorParam("TOTP 驗證碼錯誤");
    }

    // Soft delete MFA configuration
    MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
    if (config != null) {
      config.setDeleted(true);
      mfaConfigDao.updateById(config);
    }

    // Soft delete all backup codes
    mfaBackupCodeDao.deleteByEmployeeId(employeeId);

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_DISABLED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("WARNING");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info("MFA disabled for employee ID: {} from IP: {}", employeeId, ipAddress);

    return ResponseDTO.ok("MFA 已成功禁用");
  }

  /**
   * Regenerate backup codes with TOTP verification (transaction method).
   *
   * <p>Atomic operation includes:
   *
   * <ul>
   *   <li>Verify TOTP token
   *   <li>Soft delete old backup codes
   *   <li>Generate new backup codes
   *   <li>Record audit log
   * </ul>
   *
   * @param employeeId Employee ID
   * @param secret Decrypted TOTP secret
   * @param totpToken TOTP token for verification
   * @param ipAddress IP address of the request
   * @param userAgent User-Agent string
   * @return MfaSetupInitVO containing new backup codes
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<MfaSetupInitVO> regenerateBackupCodesWithVerificationTransaction(
      Long employeeId, String secret, String totpToken, String ipAddress, String userAgent) {

    // Verify TOTP
    boolean verified =
        TotpUtils.verifyTotpToken(secret, totpToken)
            .getOrElseThrow(e -> new RuntimeException("Failed to verify TOTP token", e));

    if (!verified) {
      // Record failed verification audit
      recordMfaVerificationAuditTransaction(
          employeeId,
          "BACKUP_CODE_REGENERATE_ATTEMPT",
          false,
          ipAddress,
          userAgent,
          "Invalid TOTP token");
      return ResponseDTO.userErrorParam("TOTP 驗證碼錯誤");
    }

    // Generate new backup codes (automatically deletes old ones)
    List<String> backupCodes =
        mfaBackupCodeManager
            .generateBackupCodes(employeeId)
            .getOrElseThrow(e -> new RuntimeException("Failed to regenerate backup codes", e));

    // Audit log
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("BACKUP_CODES_REGENERATED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("INFO");
    auditLog.setIpAddress(ipAddress);
    auditLog.setUserAgent(userAgent);
    auditLog.setDeleted(false);
    mfaAuditLogDao.insert(auditLog);

    log.info("Backup codes regenerated for employee ID: {} from IP: {}", employeeId, ipAddress);

    // Build response
    MfaSetupInitVO vo = new MfaSetupInitVO();
    vo.setBackupCodes(backupCodes);

    return ResponseDTO.ok(vo);
  }
}
