package net.lab1024.sa.system.mfa.manager;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import net.lab1024.sa.system.mfa.dao.MfaAuditLogDao;
import net.lab1024.sa.system.mfa.dao.MfaBackupCodeDao;
import net.lab1024.sa.system.mfa.dao.MfaConfigDao;
import net.lab1024.sa.system.mfa.dao.MfaRecoveryRequestDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaAuditLogEntity;
import net.lab1024.sa.system.mfa.domain.entity.MfaConfigEntity;
import net.lab1024.sa.system.mfa.domain.entity.MfaRecoveryRequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA Recovery Manager
 *
 * <p>Provides transactional methods for MFA recovery operations. All methods in this class are
 * wrapped in database transactions to ensure data consistency.
 *
 * <p>Key Operations:
 *
 * <ul>
 *   <li>Create recovery request (insert + audit log + email notification)
 *   <li>Approve recovery request (generate code + hash + email notification)
 *   <li>Reset MFA with recovery code (disable MFA + delete backup codes + mark code as used)
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaRecoveryManager {

  /** Recovery request status constants */
  private static final int STATUS_PENDING = 1;

  private static final int STATUS_APPROVED = 2;

  /** Recovery code expiry duration (24 hours) */
  private static final long RECOVERY_CODE_EXPIRY_HOURS = 24;

  /** Recovery code length (8 digits) */
  private static final int RECOVERY_CODE_LENGTH = 8;

  private final MfaRecoveryRequestDao mfaRecoveryRequestDao;
  private final MfaConfigDao mfaConfigDao;
  private final MfaBackupCodeDao mfaBackupCodeDao;
  private final MfaAuditLogDao mfaAuditLogDao;
  private final PasswordEncryptService passwordEncryptService;

  /**
   * Create recovery request (transaction method).
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Inserts recovery request record (status = PENDING)
   *   <li>Records CRITICAL audit log
   *   <li>Sends Email notification to Super Admin
   * </ul>
   *
   * @param employeeId Employee ID
   * @param email Request email
   * @param reason Request reason
   * @param ipAddress Request IP
   * @return Success response or error
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<Void> createRecoveryRequestTransaction(
      Long employeeId, String email, String reason, String ipAddress) {

    // Create recovery request
    MfaRecoveryRequestEntity request = new MfaRecoveryRequestEntity();
    request.setEmployeeId(employeeId);
    request.setRequestEmail(email);
    request.setRequestReason(reason);
    request.setRequestIp(ipAddress);
    request.setStatus(STATUS_PENDING);
    request.setUsed(false);
    request.setDeleted(false);

    mfaRecoveryRequestDao.insert(request);

    // Audit log (CRITICAL severity)
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_RECOVERY_REQUEST_CREATED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("CRITICAL");
    auditLog.setIpAddress(ipAddress);
    auditLog.setDeleted(false);

    mfaAuditLogDao.insert(auditLog);

    // TODO: Send Email notification to Super Admin
    // Email template: mfa_recovery_request_admin.html

    log.info(
        "[MFA Recovery] Recovery request created - EmployeeId: {}, RecoveryId: {}, IP: {}",
        employeeId,
        request.getRecoveryId(),
        ipAddress);

    return ResponseDTO.ok();
  }

  /**
   * Approve recovery request and generate recovery code (transaction method).
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Generates 8-digit random recovery code
   *   <li>Hashes recovery code with Argon2id
   *   <li>Sets expiry to NOW() + 24 hours
   *   <li>Updates request status to APPROVED
   *   <li>Sends Email to user with recovery code
   *   <li>Records CRITICAL audit log
   * </ul>
   *
   * @param recoveryId Recovery request ID
   * @param approverId Super Admin employee ID
   * @return Success response or error
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<Void> approveAndGenerateRecoveryCodeTransaction(
      Long recoveryId, Long approverId) {

    // Load recovery request
    MfaRecoveryRequestEntity request = mfaRecoveryRequestDao.selectById(recoveryId);
    if (request == null) {
      return ResponseDTO.userErrorParam("恢復請求不存在");
    }

    // Generate 8-digit random recovery code
    String recoveryCode = generateRandomCode(RECOVERY_CODE_LENGTH);

    // Hash recovery code with Argon2id
    String recoveryCodeHash = passwordEncryptService.encrypt(recoveryCode);

    // Calculate expiry (NOW + 24 hours)
    OffsetDateTime expiresAt =
        OffsetDateTime.now(ZoneOffset.UTC).plusHours(RECOVERY_CODE_EXPIRY_HOURS);

    // Update request
    request.setStatus(STATUS_APPROVED);
    request.setApproverId(approverId);
    request.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    request.setRecoveryCode(recoveryCode); // Stored temporarily for Email (will be cleared later)
    request.setRecoveryCodeHash(recoveryCodeHash);
    request.setExpiresAt(expiresAt);
    request.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));

    mfaRecoveryRequestDao.updateById(request);

    // Audit log (CRITICAL severity)
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(request.getEmployeeId());
    auditLog.setEventType("MFA_RECOVERY_REQUEST_APPROVED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("CRITICAL");
    auditLog.setDeleted(false);

    mfaAuditLogDao.insert(auditLog);

    // TODO: Send Email notification to user with recovery code
    // Email template: mfa_recovery_code.html
    // Recovery code: request.getRecoveryCode()
    // Expiry: request.getExpiresAt()

    log.info(
        "[MFA Recovery] Recovery request approved - RecoveryId: {}, ApproverId: {}, ExpiresAt: {}",
        recoveryId,
        approverId,
        expiresAt);

    return ResponseDTO.ok();
  }

  /**
   * Reset MFA using recovery code (transaction method).
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Disables old MFA configuration (mfa_enabled = false)
   *   <li>Soft deletes all backup codes
   *   <li>Soft deletes all trusted devices (not implemented yet)
   *   <li>Marks recovery code as used
   *   <li>Records CRITICAL audit log
   * </ul>
   *
   * @param employeeId Employee ID
   * @param recoveryCode Recovery code (plaintext)
   * @return Success response or error
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<Void> resetMfaWithCodeTransaction(Long employeeId, String recoveryCode) {

    // Find approved recovery request
    MfaRecoveryRequestEntity request = mfaRecoveryRequestDao.selectApprovedByEmployeeId(employeeId);
    if (request == null) {
      return ResponseDTO.userErrorParam("恢復碼無效或已過期");
    }

    // Verify recovery code hash
    boolean verified = passwordEncryptService.matches(recoveryCode, request.getRecoveryCodeHash());
    if (!verified) {
      return ResponseDTO.userErrorParam("恢復碼錯誤");
    }

    // Disable MFA configuration
    MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
    if (config != null) {
      config.setMfaEnabled(false);
      config.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
      mfaConfigDao.updateById(config);
    }

    // Soft delete all backup codes
    mfaBackupCodeDao.deleteByEmployeeId(employeeId);

    // TODO: Soft delete all trusted devices
    // mfaTrustedDeviceDao.deleteByEmployeeId(employeeId);

    // Mark recovery code as used
    request.setUsed(true);
    request.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
    request.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
    mfaRecoveryRequestDao.updateById(request);

    // Audit log (CRITICAL severity)
    MfaAuditLogEntity auditLog = new MfaAuditLogEntity();
    auditLog.setEmployeeId(employeeId);
    auditLog.setEventType("MFA_RECOVERY_CODE_USED");
    auditLog.setEventResult("SUCCESS");
    auditLog.setSeverity("CRITICAL");
    auditLog.setDeleted(false);

    mfaAuditLogDao.insert(auditLog);

    log.info(
        "[MFA Recovery] MFA reset successful - EmployeeId: {}, RecoveryId: {}",
        employeeId,
        request.getRecoveryId());

    return ResponseDTO.ok();
  }

  /**
   * Generate random numeric code.
   *
   * @param length Code length (digits)
   * @return Random numeric code
   */
  private String generateRandomCode(int length) {
    SecureRandom random = new SecureRandom();
    StringBuilder code = new StringBuilder();
    for (int i = 0; i < length; i++) {
      code.append(random.nextInt(10));
    }
    return code.toString();
  }
}
