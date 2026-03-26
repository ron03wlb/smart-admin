package net.lab1024.sa.igaming.agent.commission.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Agent commission config entity - Commission rate configuration by agent level and product type.
 *
 * <p>Defines commission rates for different combinations of agent levels (1-5) and product types
 * (SPORTS, CASINO, LIVE, POKER, LOTTERY). Supports effective date ranges for historical rate
 * tracking.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_commission_config")
public class AgentCommissionConfigEntity extends SmartAdminBaseEntity {

  /** Config ID (auto-generated). */
  @TableId(type = IdType.AUTO)
  private Long configId;

  /** Agent level (1-5). */
  private Integer agentLevel;

  /**
   * Product type.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>SPORTS - Sports betting
   *   <li>CASINO - Casino games
   *   <li>LIVE - Live casino
   *   <li>POKER - Poker games
   *   <li>LOTTERY - Lottery games
   * </ul>
   */
  private String productType;

  /**
   * Commission rate (0.0000-1.0000).
   *
   * <p>Example: 0.3000 = 30% commission
   */
  private BigDecimal commissionRate;

  /**
   * Minimum negative profit threshold (DECIMAL 19,4).
   *
   * <p>Commission is only paid if negative profit exceeds this amount. Prevents commission payout
   * on negligible losses.
   */
  private BigDecimal minNegativeProfit;

  /** Effective start date (inclusive). */
  private LocalDate effectiveFrom;

  /**
   * Effective end date (exclusive).
   *
   * <p>NULL means the config is permanently effective.
   */
  private LocalDate effectiveTo;

  /**
   * Status (1=ENABLED, 0=DISABLED).
   *
   * <p>Disabled configs are not used in commission calculation.
   */
  private Integer status;

  /** Logical delete flag (MyBatis Plus). */
  @TableLogic private Boolean deleted;
}
