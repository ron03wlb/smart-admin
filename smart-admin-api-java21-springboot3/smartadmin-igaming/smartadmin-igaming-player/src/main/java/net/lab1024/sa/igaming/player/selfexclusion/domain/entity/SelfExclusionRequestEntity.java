package net.lab1024.sa.igaming.player.selfexclusion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Self-Exclusion Request Entity.
 *
 * <p>Represents a player's self-exclusion request for responsible gaming compliance.
 *
 * <p>Supports 4 exclusion types:
 *
 * <ul>
 *   <li>DEPOSIT - Block deposits only
 *   <li>BETTING - Block bets only
 *   <li>LOGIN - Block login
 *   <li>FULL_BLOCK - Block all activities
 * </ul>
 *
 * <p>Supports 5 duration types:
 *
 * <ul>
 *   <li>24_HOURS - 24 hours
 *   <li>7_DAYS - 7 days
 *   <li>30_DAYS - 30 days
 *   <li>6_MONTHS - 6 months
 *   <li>PERMANENT - Permanent (requires special approval to remove)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_self_exclusion_request", autoResultMap = true)
public class SelfExclusionRequestEntity {

  /** Request ID (Primary Key) */
  @TableId(type = IdType.AUTO)
  private Long requestId;

  /** Tenant ID (Multi-tenancy) */
  private Long tenantId;

  /** Player ID */
  private Long playerId;

  /**
   * Exclusion Type.
   *
   * <p>Values: DEPOSIT, BETTING, LOGIN, FULL_BLOCK
   */
  private String exclusionType;

  /**
   * Duration Type.
   *
   * <p>Values: 24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT
   */
  private String durationType;

  /** Start Date (when exclusion takes effect) */
  private OffsetDateTime startDate;

  /** End Date (NULL for PERMANENT exclusions) */
  private OffsetDateTime endDate;

  /** Cooling-off Period End (earliest date player can request removal) */
  private OffsetDateTime coolingOffPeriodEnd;

  /**
   * Status.
   *
   * <p>Values: ACTIVE, EXPIRED, REMOVED, CANCELLED
   */
  private String status;

  /** Request Reason (player's reason for self-exclusion, optional) */
  private String requestReason;

  /** Removal Request Date (when player requested removal) */
  private OffsetDateTime removalRequestDate;

  /** Removal Approved By (employee ID who approved removal) */
  private Long removalApprovedBy;

  /** Removal Approved At (when removal was approved) */
  private OffsetDateTime removalApprovedAt;

  /** Removal Reason (admin's reason for approval/rejection or cancellation reason) */
  private String removalReason;

  /** IP Address (IPv4/IPv6 when request was created) */
  private String ipAddress;

  /** User Agent (browser/device information) */
  private String userAgent;

  /**
   * Compliance Acknowledgement.
   *
   * <p>TRUE if player acknowledged responsible gaming policy
   */
  @TableField(jdbcType = JdbcType.SMALLINT, typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean complianceAcknowledgement;

  /** Soft Delete Flag */
  @TableLogic
  @TableField(jdbcType = JdbcType.SMALLINT, typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean deleted;

  /** Create Time */
  private OffsetDateTime createTime;

  /** Update Time */
  private OffsetDateTime updateTime;

  /** Created By (employee ID if admin-created, NULL if player self-created) */
  private Long createdBy;
}
