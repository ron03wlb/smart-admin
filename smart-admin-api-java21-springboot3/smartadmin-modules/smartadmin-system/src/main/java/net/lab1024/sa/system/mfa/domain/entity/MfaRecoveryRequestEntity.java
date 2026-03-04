package net.lab1024.sa.system.mfa.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * MFA Recovery Request Entity
 *
 * <p>Stores device loss recovery requests when users lose access to their MFA device and backup
 * codes. Requires Super Admin approval to generate a temporary 24-hour recovery code.
 *
 * <p>Security Rules:
 *
 * <ul>
 *   <li>One PENDING request per employee (enforced by partial unique index)
 *   <li>Recovery code expires 24 hours after approval
 *   <li>Recovery code can only be used once
 *   <li>Recovery code is Argon2id hashed before storage
 *   <li>All operations logged at CRITICAL severity level
 * </ul>
 *
 * <p>Status Flow:
 *
 * <pre>
 * PENDING (1) → APPROVED (2) → (Recovery code used)
 *            ↘ REJECTED (3)
 *            ↘ EXPIRED (4)
 * </pre>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_mfa_recovery_request")
public class MfaRecoveryRequestEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long recoveryId;

  /** Employee ID requesting MFA recovery */
  private Long employeeId;

  /** Request email (for verification and notification) */
  private String requestEmail;

  /** Request reason (free text explanation) */
  private String requestReason;

  /** Request IP address (for audit) */
  private String requestIp;

  /**
   * Request status:
   *
   * <ul>
   *   <li>1 = PENDING (awaiting admin approval)
   *   <li>2 = APPROVED (recovery code generated)
   *   <li>3 = REJECTED (admin denied request)
   *   <li>4 = EXPIRED (recovery code expired after 24h)
   * </ul>
   */
  private Integer status;

  /** Approver employee ID (Super Admin who approved/rejected) */
  private Long approverId;

  /** Approval timestamp (when admin approved/rejected) */
  private OffsetDateTime approvedAt;

  /** Rejection reason (if status = REJECTED) */
  private String rejectionReason;

  /** Recovery code plaintext (8-digit random number, transient field for Email only) */
  private String recoveryCode;

  /** Recovery code hash (Argon2id hashed, persisted for verification) */
  private String recoveryCodeHash;

  /** Recovery code expiration timestamp (approval time + 24 hours) */
  private OffsetDateTime expiresAt;

  /** Recovery code used flag (prevents reuse) */
  private Boolean used;

  /** Recovery code used timestamp (when code was consumed) */
  private OffsetDateTime usedAt;

  /** Soft delete flag */
  private Boolean deleted;

  /** Optimistic lock version */
  @Version private Integer version;
}
