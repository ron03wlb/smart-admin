package net.lab1024.sa.igaming.activity.turnover.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Turnover risk action rule entity — configures risk engine actions for turnover calculation (Layer
 * 1: Risk Engine).
 *
 * <p>This entity defines what happens when a player's risk level exceeds certain thresholds:
 *
 * <ul>
 *   <li><b>PASS (Risk Level 1):</b> Normal operation - 100% turnover, betting allowed
 *   <li><b>FLAG (Risk Level 2):</b> Mark for review - 100% turnover, betting allowed, create risk
 *       proposal
 *   <li><b>BLOCK (Risk Level 3):</b> Prevent betting - 0% turnover, betting blocked
 * </ul>
 *
 * <p><b>Risk Engine Integration:</b> This rule works in conjunction with the risk scoring system
 * ({@code t_risk_score}, {@code t_risk_assessment}) to make automated decisions.
 *
 * <p><b>Database Table:</b> t_turnover_risk_action_rule
 *
 * <p><b>Multi-Tenant Isolation:</b> Row-Level Security (RLS) enforced at database layer
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_turnover_risk_action_rule")
public class TurnoverRiskActionRuleEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long ruleId;

  /** Rule code (unique per tenant). Example: "RA_PASS", "RA_FLAG", "RA_BLOCK" */
  private String ruleCode;

  /** Rule name (display name). Example: "通過 - 正常計算流水" */
  private String ruleName;

  /** Risk level (1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL). */
  private Integer riskLevel;

  /**
   * Action type. See {@link net.lab1024.sa.igaming.activity.turnover.constant.RiskActionTypeEnum}.
   */
  private Integer actionType;

  /** Turnover factor (0.00 ~ 100.00). DECIMAL(5,2) precision. */
  private BigDecimal turnoverFactor;

  /** Allow bet flag (true = allow, false = block). */
  private Boolean allowBet;

  /** Create risk proposal flag (true = create, false = no action). */
  private Boolean createProposal;

  /** Effective from timestamp (nullable - applies immediately if null). */
  private OffsetDateTime effectiveFrom;

  /** Effective to timestamp (nullable - never expires if null). */
  private OffsetDateTime effectiveTo;

  /**
   * Status. See {@link net.lab1024.sa.igaming.activity.turnover.constant.TurnoverRuleStatusEnum}.
   */
  private Integer status;

  /** Remark (business notes). */
  private String remark;

  /** Soft delete flag. */
  private Boolean deleted;

  /** Optimistic lock version. */
  @Version private Integer version;
}
