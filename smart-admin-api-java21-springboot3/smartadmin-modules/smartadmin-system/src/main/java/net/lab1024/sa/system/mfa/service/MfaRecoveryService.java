package net.lab1024.sa.system.mfa.service;

import io.vavr.control.Try;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartStringUtil;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.mfa.dao.MfaRecoveryRequestDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaRecoveryRequestEntity;
import net.lab1024.sa.system.mfa.domain.vo.MfaRecoveryRequestVO;
import net.lab1024.sa.system.mfa.manager.MfaRecoveryManager;
import org.springframework.stereotype.Service;

/**
 * MFA Recovery Service
 *
 * <p>Handles MFA device loss recovery requests with admin approval workflow.
 *
 * <p>Recovery Flow:
 *
 * <pre>
 * 1. User creates recovery request (with reason)
 * 2. Super Admin reviews pending requests
 * 3. Admin approves → 8-digit recovery code generated (24h expiry)
 * 4. Admin rejects → User receives rejection notification
 * 5. User uses recovery code → MFA reset successful
 * </pre>
 *
 * <p>Security Rules:
 *
 * <ul>
 *   <li>One PENDING request per employee at a time
 *   <li>Recovery code expires 24 hours after approval
 *   <li>Recovery code is Argon2id hashed
 *   <li>Recovery code can only be used once
 *   <li>All operations logged at CRITICAL severity
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaRecoveryService {

  /** Recovery request status constants */
  private static final int STATUS_PENDING = 1;

  private static final int STATUS_APPROVED = 2;
  private static final int STATUS_REJECTED = 3;
  private static final int STATUS_EXPIRED = 4;

  /** Recovery code expiry duration (24 hours) */
  private static final long RECOVERY_CODE_EXPIRY_HOURS = 24;

  private final MfaRecoveryRequestDao mfaRecoveryRequestDao;
  private final MfaRecoveryManager mfaRecoveryManager;
  private final EmployeeDao employeeDao;
  private final PasswordEncryptService passwordEncryptService;

  /**
   * Create a new MFA recovery request.
   *
   * <p>Validation Rules:
   *
   * <ul>
   *   <li>Employee must exist and not be deleted
   *   <li>MFA must be currently enabled for this employee
   *   <li>No existing PENDING request for this employee
   * </ul>
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Creates recovery request record (status = PENDING)
   *   <li>Sends Email notification to Super Admin
   *   <li>Records CRITICAL audit log
   * </ul>
   *
   * @param employeeId Employee ID requesting recovery
   * @param email Request email (for verification)
   * @param reason Request reason (free text explanation)
   * @param ipAddress Request IP address (for audit)
   * @return Success response or error
   */
  public ResponseDTO<Void> createRecoveryRequest(
      Long employeeId, String email, String reason, String ipAddress) {

    // Validate employee exists
    EmployeeEntity employee = employeeDao.selectById(employeeId);
    if (employee == null || Boolean.TRUE.equals(employee.getDeleted())) {
      return ResponseDTO.userErrorParam("員工不存在");
    }

    // Check if employee has a pending request
    MfaRecoveryRequestEntity existingRequest =
        mfaRecoveryRequestDao.selectByEmployeeIdAndStatus(employeeId, STATUS_PENDING);
    if (existingRequest != null) {
      return ResponseDTO.userErrorParam("您已有待審核的恢復請求，請等待管理員處理");
    }

    // Delegate to Manager for transactional operation
    return mfaRecoveryManager.createRecoveryRequestTransaction(
        employeeId, email, reason, ipAddress);
  }

  /**
   * Get all pending recovery requests (for Super Admin).
   *
   * @return List of pending recovery requests with employee details
   */
  public ResponseDTO<List<MfaRecoveryRequestVO>> getPendingRequests() {
    List<MfaRecoveryRequestEntity> pendingRequests = mfaRecoveryRequestDao.selectPendingRequests();

    // Convert to VO with employee details
    List<MfaRecoveryRequestVO> resultList =
        pendingRequests.stream()
            .map(
                request -> {
                  MfaRecoveryRequestVO vo = new MfaRecoveryRequestVO();
                  vo.setRecoveryId(request.getRecoveryId());
                  vo.setEmployeeId(request.getEmployeeId());

                  // Load employee details
                  EmployeeEntity employee = employeeDao.selectById(request.getEmployeeId());
                  if (employee != null) {
                    vo.setEmployeeName(employee.getActualName());
                    vo.setDepartmentId(employee.getDepartmentId());
                  }

                  vo.setRequestEmail(request.getRequestEmail());
                  vo.setRequestReason(request.getRequestReason());
                  vo.setRequestIp(request.getRequestIp());
                  vo.setStatus(request.getStatus());
                  vo.setCreateTime(request.getCreateTime());

                  return vo;
                })
            .toList();

    return ResponseDTO.ok(resultList);
  }

  /**
   * Approve a recovery request (Super Admin only).
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Generates 8-digit random recovery code
   *   <li>Hashes recovery code with Argon2id
   *   <li>Sets expiry to NOW() + 24 hours
   *   <li>Sends Email to user with recovery code
   *   <li>Updates request status to APPROVED
   *   <li>Records CRITICAL audit log
   * </ul>
   *
   * @param recoveryId Recovery request ID
   * @param approverId Super Admin employee ID
   * @return Success response or error
   */
  public ResponseDTO<Void> approveRecoveryRequest(Long recoveryId, Long approverId) {

    // Validate request exists and is PENDING
    MfaRecoveryRequestEntity request =
        mfaRecoveryRequestDao.selectByIdAndStatus(recoveryId, STATUS_PENDING);
    if (request == null) {
      return ResponseDTO.userErrorParam("恢復請求不存在或狀態不正確");
    }

    // Delegate to Manager for transactional operation
    return mfaRecoveryManager.approveAndGenerateRecoveryCodeTransaction(recoveryId, approverId);
  }

  /**
   * Reject a recovery request (Super Admin only).
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Updates request status to REJECTED
   *   <li>Records rejection reason
   *   <li>Sends Email notification to user
   *   <li>Records WARNING audit log
   * </ul>
   *
   * @param recoveryId Recovery request ID
   * @param approverId Super Admin employee ID
   * @param rejectionReason Reason for rejection
   * @return Success response or error
   */
  public ResponseDTO<Void> rejectRecoveryRequest(
      Long recoveryId, Long approverId, String rejectionReason) {

    // Validate request exists and is PENDING
    MfaRecoveryRequestEntity request =
        mfaRecoveryRequestDao.selectByIdAndStatus(recoveryId, STATUS_PENDING);
    if (request == null) {
      return ResponseDTO.userErrorParam("恢復請求不存在或狀態不正確");
    }

    // Update request to REJECTED status
    request.setStatus(STATUS_REJECTED);
    request.setApproverId(approverId);
    request.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    request.setRejectionReason(rejectionReason);
    request.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));

    mfaRecoveryRequestDao.updateById(request);

    // TODO: Send Email notification to user (rejection)
    // TODO: Record WARNING audit log

    log.info(
        "[MFA Recovery] Request rejected - RecoveryId: {}, ApproverId: {}, Reason: {}",
        recoveryId,
        approverId,
        rejectionReason);

    return ResponseDTO.ok();
  }

  /**
   * Verify recovery code.
   *
   * <p>Used during MFA reset flow to validate the recovery code entered by user.
   *
   * @param employeeId Employee ID
   * @param recoveryCode 8-digit recovery code (plaintext)
   * @return true if code is valid, false otherwise
   */
  public Try<Boolean> verifyRecoveryCode(Long employeeId, String recoveryCode) {
    return Try.of(
        () -> {
          // Validate recovery code format (8 digits)
          if (SmartStringUtil.isEmpty(recoveryCode) || recoveryCode.length() != 8) {
            log.warn(
                "[MFA Recovery] Invalid recovery code format - EmployeeId: {}", employeeId);
            return false;
          }

          // Find approved recovery request (non-expired, non-used)
          MfaRecoveryRequestEntity request =
              mfaRecoveryRequestDao.selectApprovedByEmployeeId(employeeId);
          if (request == null) {
            log.warn(
                "[MFA Recovery] No valid recovery request found - EmployeeId: {}", employeeId);
            return false;
          }

          // Verify recovery code hash
          boolean verified =
              passwordEncryptService.matches(recoveryCode, request.getRecoveryCodeHash());

          if (!verified) {
            log.warn(
                "[MFA Recovery] Recovery code verification failed - EmployeeId: {}", employeeId);
          }

          return verified;
        });
  }

  /**
   * Reset MFA using recovery code.
   *
   * <p>Flow:
   *
   * <ul>
   *   <li>Verify recovery code (Argon2id hash)
   *   <li>Check expiry (< 24 hours)
   *   <li>Disable old MFA configuration
   *   <li>Soft delete old backup codes
   *   <li>Soft delete trusted devices
   *   <li>Mark recovery code as used
   *   <li>Record CRITICAL audit log
   * </ul>
   *
   * @param employeeId Employee ID
   * @param recoveryCode 8-digit recovery code (plaintext)
   * @return Success response or error
   */
  public ResponseDTO<Void> resetMfaWithRecoveryCode(Long employeeId, String recoveryCode) {

    // Verify recovery code
    Try<Boolean> verificationResult = verifyRecoveryCode(employeeId, recoveryCode);
    if (verificationResult.isFailure() || !verificationResult.get()) {
      return ResponseDTO.userErrorParam("恢復碼無效或已過期");
    }

    // Delegate to Manager for transactional operation
    return mfaRecoveryManager.resetMfaWithCodeTransaction(employeeId, recoveryCode);
  }
}
