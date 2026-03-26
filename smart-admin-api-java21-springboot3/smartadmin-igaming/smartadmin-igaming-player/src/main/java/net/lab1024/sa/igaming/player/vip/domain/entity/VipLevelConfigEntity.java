package net.lab1024.sa.igaming.player.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * VIP Level Configuration Entity.
 *
 * <p>Defines VIP level upgrade requirements and benefits. Each VIP level has specific thresholds
 * for cumulative deposit, turnover, and active days. Benefits include withdrawal limits, rebate
 * rates, bonuses, and extra perks stored in JSON.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_vip_level_config", autoResultMap = true)
public class VipLevelConfigEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long configId;

  /** VIP level number (1-10, higher is better). */
  private Integer vipLevel;

  /** VIP level display name (Bronze, Silver, Gold, etc.). */
  private String levelName;

  /** VIP level description. */
  private String levelDescription;

  // ============================================================
  // Upgrade Requirements
  // ============================================================

  /** Minimum cumulative deposit amount required to reach this level. */
  private BigDecimal upgradeDepositRequirement;

  /** Minimum cumulative valid turnover required to reach this level. */
  private BigDecimal upgradeTurnoverRequirement;

  /** Minimum active days count required to reach this level. */
  private Integer upgradeActiveDaysRequirement;

  // ============================================================
  // VIP Benefits
  // ============================================================

  /** Daily withdrawal limit for this VIP level (NULL = unlimited). */
  private BigDecimal dailyWithdrawalLimit;

  /** Monthly withdrawal limit for this VIP level (NULL = unlimited). */
  private BigDecimal monthlyWithdrawalLimit;

  /** Rebate rate for this VIP level (decimal, e.g., 0.0050 = 0.5%). */
  private BigDecimal rebateRate;

  /** Birthday bonus amount for this VIP level. */
  private BigDecimal birthdayBonus;

  /** One-time bonus when player reaches this VIP level. */
  private BigDecimal levelUpBonus;

  /**
   * Additional benefits in JSON format.
   *
   * <p>Example: {"priority_support": true, "dedicated_account_manager": true, "exclusive_events":
   * true}
   */
  @TableField(typeHandler = JacksonTypeHandler.class)
  private Map<String, Object> extraBenefits;

  // ============================================================
  // Validity Period
  // ============================================================

  /** Effective start date. */
  private LocalDate effectiveFrom;

  /** Effective end date (NULL = no expiry). */
  private LocalDate effectiveTo;

  // ============================================================
  // Audit Fields
  // ============================================================

  /** Status (1: ACTIVE, 0: INACTIVE). */
  private Integer status;

  /** Soft delete flag (MyBatis Plus @TableLogic). */
  @TableLogic(value = "false", delval = "true")
  private Boolean deleted;
}
