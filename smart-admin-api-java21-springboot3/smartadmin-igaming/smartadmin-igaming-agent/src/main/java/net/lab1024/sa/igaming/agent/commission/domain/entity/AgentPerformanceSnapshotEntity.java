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
 * Agent performance snapshot entity - Weekly/monthly performance metrics for agents.
 *
 * <p>Records periodic performance snapshots for agents, including player counts, bet amounts,
 * turnover, and commission. Used for rankings, reports, and dashboard analytics.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_performance_snapshot")
public class AgentPerformanceSnapshotEntity extends SmartAdminBaseEntity {

  /** Snapshot ID (auto-generated). */
  @TableId(type = IdType.AUTO)
  private Long snapshotId;

  /** Agent ID (FK → t_player). */
  private Long agentId;

  /**
   * Snapshot type.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>WEEKLY - Weekly snapshot (last day of week)
   *   <li>MONTHLY - Monthly snapshot (last day of month)
   * </ul>
   */
  private String snapshotType;

  /**
   * Snapshot date (last day of period).
   *
   * <p>For WEEKLY: Sunday (end of week). For MONTHLY: Last day of month (e.g., 2026-03-31).
   */
  private LocalDate snapshotDate;

  /** Total player count under this agent. */
  private Integer totalPlayersCount;

  /**
   * Active player count (players with bets in this period).
   *
   * <p>Used to calculate agent engagement and player retention.
   */
  private Integer activePlayersCount;

  /** New player count (players registered in this period). */
  private Integer newPlayersCount;

  /** Total deposit amount (DECIMAL 19,4). */
  private BigDecimal totalDepositAmount;

  /** Total withdrawal amount (DECIMAL 19,4). */
  private BigDecimal totalWithdrawalAmount;

  /** Total bet amount (DECIMAL 19,4). */
  private BigDecimal totalBetAmount;

  /** Total valid turnover (DECIMAL 19,4). */
  private BigDecimal totalValidTurnover;

  /**
   * Total negative profit (DECIMAL 19,4).
   *
   * <p>Calculated as: totalBetAmount - totalPayoutAmount (player losses).
   */
  private BigDecimal totalNegativeProfit;

  /** Total commission amount for this period (DECIMAL 19,4). */
  private BigDecimal totalCommissionAmount;

  /** Logical delete flag (MyBatis Plus). */
  @TableLogic(value = "false", delval = "true")
  private Boolean deleted;
}
