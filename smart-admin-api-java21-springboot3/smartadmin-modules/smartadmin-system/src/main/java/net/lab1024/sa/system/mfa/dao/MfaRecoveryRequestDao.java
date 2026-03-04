package net.lab1024.sa.system.mfa.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.system.mfa.domain.entity.MfaRecoveryRequestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MFA Recovery Request DAO
 *
 * <p>Provides database access methods for MFA device loss recovery request management.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Mapper
public interface MfaRecoveryRequestDao extends BaseMapper<MfaRecoveryRequestEntity> {

  /**
   * Select recovery request by employee ID and status (non-deleted records only).
   *
   * <p>Used to check if employee has a PENDING request before creating a new one.
   *
   * @param employeeId Employee ID
   * @param status Request status (1=PENDING, 2=APPROVED, 3=REJECTED, 4=EXPIRED)
   * @return Recovery request entity or null if not found
   */
  MfaRecoveryRequestEntity selectByEmployeeIdAndStatus(
      @Param("employeeId") Long employeeId, @Param("status") Integer status);

  /**
   * Select all pending recovery requests (non-deleted records only).
   *
   * <p>Used by Super Admin to view pending approval queue.
   *
   * @return List of pending recovery requests ordered by create time ascending
   */
  List<MfaRecoveryRequestEntity> selectPendingRequests();

  /**
   * Select recovery request by ID and status (non-deleted records only).
   *
   * <p>Used for approval/rejection operations with status verification.
   *
   * @param recoveryId Recovery request ID
   * @param status Expected status (1=PENDING, 2=APPROVED)
   * @return Recovery request entity or null if not found
   */
  MfaRecoveryRequestEntity selectByIdAndStatus(
      @Param("recoveryId") Long recoveryId, @Param("status") Integer status);

  /**
   * Batch update expired recovery codes to EXPIRED status.
   *
   * <p>Scheduled job runs hourly to mark expired codes (expires_at < NOW() and status = APPROVED
   * and used = false).
   *
   * @return Number of records updated
   */
  int updateExpiredRecoveryCodes();

  /**
   * Select approved recovery request by employee ID (non-deleted, non-used, non-expired).
   *
   * <p>Used to verify recovery code during MFA reset flow.
   *
   * @param employeeId Employee ID
   * @return Approved recovery request entity or null if not found
   */
  MfaRecoveryRequestEntity selectApprovedByEmployeeId(@Param("employeeId") Long employeeId);
}
