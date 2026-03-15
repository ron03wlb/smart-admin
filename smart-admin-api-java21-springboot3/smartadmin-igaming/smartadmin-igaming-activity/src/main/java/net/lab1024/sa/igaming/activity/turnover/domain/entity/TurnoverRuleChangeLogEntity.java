package net.lab1024.sa.igaming.activity.turnover.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Turnover rule change log entity — audit trail for all turnover rule modifications.
 *
 * <p>This entity records all changes to turnover rules for compliance and debugging purposes:
 *
 * <ul>
 *   <li>Who made the change (operator_id, operator_name)
 *   <li>What was changed (old_value vs new_value as PostgreSQL JSONB)
 *   <li>When it was changed (create_time)
 *   <li>Why it was changed (change_reason)
 * </ul>
 *
 * <p><b>JSONB Storage:</b> PostgreSQL JSONB fields (old_value, new_value) store complete rule
 * snapshots for full audit history.
 *
 * <p><b>Database Table:</b> t_turnover_rule_change_log
 *
 * <p><b>Multi-Tenant Isolation:</b> Row-Level Security (RLS) enforced at database layer
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_turnover_rule_change_log")
public class TurnoverRuleChangeLogEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long logId;

  /**
   * Rule type: 1=GameWeight, 2=StatusFactor, 3=OddsThreshold, 4=RiskAction.
   *
   * <p>Maps to table names:
   *
   * <ul>
   *   <li>1 → t_turnover_game_weight_rule
   *   <li>2 → t_turnover_status_factor_rule
   *   <li>3 → t_turnover_odds_threshold_rule
   *   <li>4 → t_turnover_risk_action_rule
   * </ul>
   */
  private Integer ruleType;

  /** Rule ID (FK to the corresponding rule table). */
  private Long ruleId;

  /** Rule code (denormalized for quick lookup). Example: "GW_SLOTS_100" */
  private String ruleCode;

  /** Operation type: 1=INSERT, 2=UPDATE, 3=DELETE, 4=RELOAD. */
  private Integer operationType;

  /** Old value (PostgreSQL JSONB). NULL for INSERT operations. */
  private String oldValue;

  /** New value (PostgreSQL JSONB). NULL for DELETE operations. */
  private String newValue;

  /** Change reason (required for UPDATE/DELETE). Example: "調整老虎機權重以符合合規要求" */
  private String changeReason;

  /** Operator ID (employee_id who made the change). */
  private Long operatorId;

  /** Operator name (employee_name, denormalized for quick display). */
  private String operatorName;

  /** Change timestamp (denormalized from create_time for performance). */
  private OffsetDateTime createTime;
}
