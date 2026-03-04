package net.lab1024.sa.system.mfa.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.system.mfa.domain.form.MfaRecoveryRejectForm;
import net.lab1024.sa.system.mfa.domain.form.MfaRecoveryRequestForm;
import net.lab1024.sa.system.mfa.domain.form.MfaRecoveryResetForm;
import net.lab1024.sa.system.mfa.domain.vo.MfaRecoveryRequestVO;
import net.lab1024.sa.system.mfa.service.MfaRecoveryService;
import org.springframework.web.bind.annotation.*;

/**
 * MFA Recovery Controller
 *
 * <p>Handles MFA device loss recovery requests with admin approval workflow.
 *
 * <p>API Endpoints:
 *
 * <ul>
 *   <li>POST /mfa/recovery/request - Create recovery request (any user with MFA enabled)
 *   <li>GET /mfa/recovery/pending - Get pending requests (Super Admin only)
 *   <li>POST /mfa/recovery/approve - Approve request (Super Admin only)
 *   <li>POST /mfa/recovery/reject - Reject request (Super Admin only)
 *   <li>POST /mfa/recovery/reset - Reset MFA with recovery code (any user)
 * </ul>
 *
 * <p>Security Notes:
 *
 * <ul>
 *   <li>All endpoints require authentication
 *   <li>Admin endpoints require Super Admin permission
 *   <li>Recovery code is single-use and expires after 24 hours
 *   <li>All operations are logged at CRITICAL severity
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MFA)
public class MfaRecoveryController {

  private final MfaRecoveryService mfaRecoveryService;

  /**
   * Create MFA recovery request.
   *
   * <p>User initiates a recovery request when they lose access to their MFA device and backup
   * codes.
   *
   * <p>Validation:
   *
   * <ul>
   *   <li>User must have MFA currently enabled
   *   <li>No existing PENDING request allowed
   *   <li>Email and reason are required
   * </ul>
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Creates recovery request (status = PENDING)
   *   <li>Sends Email notification to Super Admin
   *   <li>Records CRITICAL audit log
   * </ul>
   *
   * @param form Recovery request form (email, reason)
   * @return Success response or error
   */
  @Operation(summary = "創建 MFA 恢復請求 @author SmartAdmin MFA Team")
  @PostMapping("/mfa/recovery/request")
  public ResponseDTO<Void> createRecoveryRequest(@Valid @RequestBody MfaRecoveryRequestForm form) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    String ipAddress = SmartRequestUtil.getRequestUser().getIp();
    return mfaRecoveryService.createRecoveryRequest(
        employeeId, form.getEmail(), form.getReason(), ipAddress);
  }

  /**
   * Get pending recovery requests (Super Admin only).
   *
   * <p>Returns list of all pending recovery requests awaiting approval.
   *
   * @return List of pending recovery requests with employee details
   */
  @Operation(summary = "查詢待審核恢復請求（Super Admin）@author SmartAdmin MFA Team")
  @GetMapping("/mfa/recovery/pending")
  @SaCheckPermission("mfa:recovery:admin")
  public ResponseDTO<List<MfaRecoveryRequestVO>> getPendingRequests() {
    return mfaRecoveryService.getPendingRequests();
  }

  /**
   * Approve recovery request (Super Admin only).
   *
   * <p>Approves a pending recovery request and generates a 24-hour recovery code.
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
   * @return Success response or error
   */
  @Operation(summary = "批准恢復請求（Super Admin）@author SmartAdmin MFA Team")
  @PostMapping("/mfa/recovery/approve")
  @SaCheckPermission("mfa:recovery:admin")
  public ResponseDTO<Void> approveRecoveryRequest(@RequestParam Long recoveryId) {
    Long approverId = SmartRequestUtil.getRequestUserId();
    return mfaRecoveryService.approveRecoveryRequest(recoveryId, approverId);
  }

  /**
   * Reject recovery request (Super Admin only).
   *
   * <p>Rejects a pending recovery request with a reason.
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
   * @param form Rejection form (recoveryId, reason)
   * @return Success response or error
   */
  @Operation(summary = "拒絕恢復請求（Super Admin）@author SmartAdmin MFA Team")
  @PostMapping("/mfa/recovery/reject")
  @SaCheckPermission("mfa:recovery:admin")
  public ResponseDTO<Void> rejectRecoveryRequest(@Valid @RequestBody MfaRecoveryRejectForm form) {
    Long approverId = SmartRequestUtil.getRequestUserId();
    return mfaRecoveryService.rejectRecoveryRequest(
        form.getRecoveryId(), approverId, form.getRejectionReason());
  }

  /**
   * Reset MFA using recovery code.
   *
   * <p>User provides the 8-digit recovery code received via Email to reset their MFA.
   *
   * <p>Validation:
   *
   * <ul>
   *   <li>Recovery code must be valid (Argon2id hash match)
   *   <li>Recovery code must not be expired (< 24 hours)
   *   <li>Recovery code must not be already used
   * </ul>
   *
   * <p>Side Effects:
   *
   * <ul>
   *   <li>Disables MFA configuration (mfa_enabled = false)
   *   <li>Soft deletes all backup codes
   *   <li>Soft deletes all trusted devices
   *   <li>Marks recovery code as used
   *   <li>Records CRITICAL audit log
   * </ul>
   *
   * @param form Recovery reset form (recoveryCode)
   * @return Success response or error
   */
  @Operation(summary = "使用恢復碼重置 MFA @author SmartAdmin MFA Team")
  @PostMapping("/mfa/recovery/reset")
  public ResponseDTO<Void> resetMfaWithRecoveryCode(@Valid @RequestBody MfaRecoveryResetForm form) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    return mfaRecoveryService.resetMfaWithRecoveryCode(employeeId, form.getRecoveryCode());
  }
}
