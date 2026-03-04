package net.lab1024.sa.system.mfa.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * MFA Audit Log Entity
 *
 * <p>Records all MFA-related events for security audit and compliance purposes. Supports tiered
 * retention policy: INFO (90 days), WARNING (1 year), CRITICAL (permanent).
 *
 * <p>Event Types:
 *
 * <ul>
 *   <li>MFA_ENABLED - User enabled MFA
 *   <li>MFA_DISABLED - User disabled MFA
 *   <li>MFA_VERIFY_SUCCESS - Successful MFA verification
 *   <li>MFA_VERIFY_FAIL - Failed MFA verification
 *   <li>BACKUP_CODE_USED - Backup code was used
 *   <li>TRUSTED_DEVICE_ADDED - New trusted device added
 *   <li>TRUSTED_DEVICE_REMOVED - Trusted device removed
 *   <li>ANOMALY_DETECTED - Anomaly detected (geo-location change, repeated failures)
 *   <li>ACCOUNT_LOCKED - Account locked due to repeated MFA failures
 *   <li>RECOVERY_INITIATED - Account recovery process initiated
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_mfa_audit_log")
public class MfaAuditLogEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long logId;

  /** Employee ID (foreign key to t_employee) */
  private Long employeeId;

  /**
   * Event type: MFA_ENABLED, MFA_DISABLED, MFA_VERIFY_SUCCESS, MFA_VERIFY_FAIL, BACKUP_CODE_USED,
   * TRUSTED_DEVICE_ADDED, TRUSTED_DEVICE_REMOVED, ANOMALY_DETECTED, ACCOUNT_LOCKED,
   * RECOVERY_INITIATED
   */
  private String eventType;

  /** Event result: SUCCESS, FAILURE */
  private String eventResult;

  /** Severity: INFO, WARNING, CRITICAL */
  private String severity;

  /** IP address when event occurred */
  private String ipAddress;

  /** User agent string */
  private String userAgent;

  /** Error message (if eventResult = FAILURE) */
  private String errorMessage;

  /** Soft delete flag */
  private Boolean deleted;
}
