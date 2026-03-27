package net.lab1024.sa.igaming.player.selfexclusion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lab1024.sa.igaming.player.typehandler.PostgresJsonbTypeHandler;

/**
 * Self-Exclusion History Entity.
 *
 * <p>Audit trail for all self-exclusion request state changes. Required for regulatory compliance.
 *
 * <p>Action Types:
 *
 * <ul>
 *   <li>CREATED - Player created request
 *   <li>ACTIVATED - Request took effect
 *   <li>EXPIRED - Auto-expired
 *   <li>REMOVAL_REQUESTED - Player requested removal
 *   <li>REMOVAL_APPROVED - Admin approved removal
 *   <li>REMOVAL_REJECTED - Admin rejected removal
 *   <li>CANCELLED - Admin cancelled request
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_self_exclusion_history", autoResultMap = true)
public class SelfExclusionHistoryEntity {

  /** History ID (Primary Key) */
  @TableId(type = IdType.AUTO)
  private Long historyId;

  /** Tenant ID (Multi-tenancy) */
  private Long tenantId;

  /** Request ID (reference to t_self_exclusion_request) */
  private Long requestId;

  /** Player ID */
  private Long playerId;

  /**
   * Action Type.
   *
   * <p>Values: CREATED, ACTIVATED, EXPIRED, REMOVAL_REQUESTED, REMOVAL_APPROVED, REMOVAL_REJECTED,
   * CANCELLED
   */
  private String actionType;

  /** Action By (employee ID, NULL for system actions like auto-expiry) */
  private Long actionBy;

  /** Action Reason (required for REMOVAL_APPROVED, REMOVAL_REJECTED, CANCELLED) */
  private String actionReason;

  /** Action At (timestamp when action occurred) */
  private OffsetDateTime actionAt;

  /** Previous Status (NULL if action type is CREATED) */
  private String previousStatus;

  /** New Status */
  private String newStatus;

  /**
   * Metadata (additional context in JSON format).
   *
   * <p>Example: {"ip_address": "1.2.3.4", "user_agent": "...", "notes": "..."}
   */
  @TableField(typeHandler = PostgresJsonbTypeHandler.class)
  private Map<String, Object> metadata;

  /** Soft Delete Flag */
  private Boolean deleted;

  /** Create Time */
  private OffsetDateTime createTime;

  /** Update Time */
  private OffsetDateTime updateTime;
}
