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
 * Agent commission record entity - Commission calculation result for each agent.
 *
 * <p>Records the commission calculation details for a specific agent, settlement period, and
 * product type. Includes bet/payout amounts, commission rate, and final commission amount.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_commission_record")
public class AgentCommissionRecordEntity extends SmartAdminBaseEntity {

  /** Record ID (auto-generated). */
  @TableId(type = IdType.AUTO)
  private Long recordId;

  /** Agent ID (FK → t_player). */
  private Long agentId;

  /**
   * Settlement date (weekly settlement).
   *
   * <p>Example: 2026-03-24 represents the week ending on 2026-03-24.
   */
  private LocalDate settlementDate;

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

  /** Total valid turnover (DECIMAL 19,4). */
  private BigDecimal totalValidTurnover;

  /** Total bet amount (DECIMAL 19,4). */
  private BigDecimal totalBetAmount;

  /** Total payout amount (DECIMAL 19,4). */
  private BigDecimal totalPayoutAmount;

  /**
   * Total negative profit (DECIMAL 19,4).
   *
   * <p>Calculated as: totalBetAmount - totalPayoutAmount (player losses).
   */
  private BigDecimal totalNegativeProfit;

  /** Applied commission rate (0.0000-1.0000). */
  private BigDecimal commissionRate;

  /**
   * Gross commission amount (DECIMAL 19,4).
   *
   * <p>Calculated as: totalNegativeProfit * commissionRate
   */
  private BigDecimal commissionAmount;

  /**
   * Platform cost deduction (DECIMAL 19,4).
   *
   * <p>Operating costs, bonuses, or other deductions from commission.
   */
  private BigDecimal platformCost;

  /**
   * Net commission amount (DECIMAL 19,4).
   *
   * <p>Calculated as: commissionAmount - platformCost (final payout amount).
   */
  private BigDecimal netCommission;

  /**
   * Record status.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>PENDING - Awaiting settlement
   *   <li>SETTLED - Commission paid
   *   <li>FROZEN - Frozen due to risk/fraud
   *   <li>CANCELLED - Settlement cancelled
   * </ul>
   */
  private String status;

  /** Settlement batch ID (FK → t_agent_commission_settlement). */
  private Long settlementBatchId;

  /**
   * Frozen reason (only if status = FROZEN).
   *
   * <p>Example: "Suspected fraud activity", "Risk review pending"
   */
  private String frozenReason;

  /** Logical delete flag (MyBatis Plus). */
  @TableLogic private Boolean deleted;
}
