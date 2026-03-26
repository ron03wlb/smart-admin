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
 * VIP Reward Record Entity.
 *
 * <p>Tracks VIP-related reward distribution including level-up bonuses, birthday bonuses, and
 * rebates. Supports reward status tracking (PENDING, ISSUED, CANCELLED, EXPIRED) and expiration
 * management.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_vip_reward_record")
public class VipRewardRecordEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long rewardId;

  /** Player ID (foreign key to t_player.player_id). */
  private Long playerId;

  /** VIP level at the time of reward. */
  private Integer vipLevel;

  // ============================================================
  // Reward Details
  // ============================================================

  /** Type of reward: LEVEL_UP_BONUS, BIRTHDAY_BONUS, MONTHLY_REBATE, WEEKLY_REBATE. */
  private String rewardType;

  /** Reward amount to be issued to player wallet. */
  private BigDecimal rewardAmount;

  /** Reward description. */
  private String rewardDescription;

  // ============================================================
  // Reward Status
  // ============================================================

  /**
   * Reward status: PENDING (awaiting issue), ISSUED (credited to wallet), CANCELLED (admin
   * cancelled), EXPIRED (past expiry date).
   */
  private String status;

  /** Timestamp when reward was issued to player wallet. */
  private OffsetDateTime issuedAt;

  /** Reward expiration timestamp (player must claim before this time). */
  private OffsetDateTime expiresAt;

  // ============================================================
  // Audit Information
  // ============================================================

  /** Employee ID who manually issued the reward (NULL for auto issued). */
  private Long issuedBy;

  /** Reason for cancellation (if status = CANCELLED). */
  private String cancellationReason;

  /** Soft delete flag (MyBatis Plus @TableLogic). */
  @TableLogic(value = "false", delval = "true")
  private Boolean deleted;
}
