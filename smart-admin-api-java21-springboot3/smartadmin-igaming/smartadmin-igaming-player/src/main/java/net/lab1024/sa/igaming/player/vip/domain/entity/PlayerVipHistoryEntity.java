package net.lab1024.sa.igaming.player.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Player VIP History Entity.
 *
 * <p>Tracks all VIP level changes for players including auto upgrades, manual upgrades, and
 * downgrades. Records player statistics at the time of upgrade for audit purposes.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_player_vip_history")
public class PlayerVipHistoryEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long historyId;

  /** Player ID (foreign key to t_player.player_id). */
  private Long playerId;

  // ============================================================
  // VIP Level Change
  // ============================================================

  /** VIP level before the change. */
  private Integer oldVipLevel;

  /** VIP level after the change. */
  private Integer newVipLevel;

  /** Type of change: AUTO_UPGRADE, MANUAL_UPGRADE, DOWNGRADE, MANUAL_DOWNGRADE. */
  private String upgradeType;

  // ============================================================
  // Player Stats at Upgrade Time
  // ============================================================

  /** Player total cumulative deposit at the time of upgrade. */
  private BigDecimal totalDepositAtUpgrade;

  /** Player total cumulative valid turnover at the time of upgrade. */
  private BigDecimal totalTurnoverAtUpgrade;

  /** Player active days count at the time of upgrade. */
  private Integer activeDaysAtUpgrade;

  // ============================================================
  // Audit Information
  // ============================================================

  /** Reason for the upgrade/downgrade. */
  private String upgradeReason;

  /** Timestamp when the upgrade occurred. */
  private OffsetDateTime upgradedAt;

  /** Employee ID who manually upgraded the player (NULL for auto upgrades). */
  private Long upgradedBy;

  /** Soft delete flag (MyBatis Plus @TableLogic). */
  @TableLogic(value = "false", delval = "true")
  private Boolean deleted;
}
