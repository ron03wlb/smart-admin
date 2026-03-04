package net.lab1024.sa.system.mfa.manager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.mfa.dao.MfaAuditLogDao;
import net.lab1024.sa.system.mfa.dao.MfaBackupCodeDao;
import net.lab1024.sa.system.mfa.dao.MfaConfigDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaAuditLogEntity;
import net.lab1024.sa.system.mfa.domain.entity.MfaConfigEntity;
import net.lab1024.sa.system.mfa.service.MfaBackupCodeService;
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
  private final MfaBackupCodeService mfaBackupCodeService;

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
        mfaBackupCodeService
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
        mfaBackupCodeService
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
}
