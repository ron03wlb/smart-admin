package net.lab1024.sa.igaming.agent.commission.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Agent commission settlement batch entity - Settlement batch with duplicate prevention.
 *
 * <p>Manages commission settlement batches for a specific period. Prevents duplicate settlements
 * through database unique constraint and Redis distributed lock.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_commission_settlement")
public class AgentCommissionSettlementEntity extends SmartAdminBaseEntity {

  /** Batch ID (auto-generated). */
  @TableId(type = IdType.AUTO)
  private Long batchId;

  /**
   * Settlement date.
   *
   * <p>Example: 2026-03-24 represents the settlement for the week 2026-03-18 ~ 2026-03-24.
   */
  private LocalDate settlementDate;

  /** Total number of agents included in this settlement batch. */
  private Integer totalAgentsCount;

  /** Total commission amount for all agents (DECIMAL 19,4). */
  private BigDecimal totalCommissionAmount;

  /**
   * Batch status.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>RUNNING - Settlement in progress
   *   <li>COMPLETED - Settlement completed successfully
   *   <li>FAILED - Settlement failed
   * </ul>
   */
  private String status;

  /** Settlement start timestamp. */
  private OffsetDateTime startedAt;

  /** Settlement completion timestamp (NULL if status != COMPLETED). */
  private OffsetDateTime completedAt;

  /**
   * Error message (only if status = FAILED).
   *
   * <p>Contains detailed error information for troubleshooting.
   */
  private String errorMessage;

  /** Logical delete flag (MyBatis Plus). */
  @TableLogic(value = "false", delval = "true")
  private Boolean deleted;
}
