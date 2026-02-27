package net.lab1024.sa.igaming.risk.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Per-transaction risk assessment log entity.
 *
 * <p>This is an append-only log table (no update_time), so it does NOT extend SmartAdminBaseEntity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@TableName("t_risk_assessment")
public class RiskAssessmentEntity {

  @TableId(type = IdType.AUTO)
  private Long assessmentId;

  private Long tenantId;

  private Long playerId;

  /** Trigger event type (e.g. BET_PLACED, WITHDRAWAL_REQUESTED). */
  private String eventType;

  private String eventId;

  /** Risk score (0-100) DECIMAL(8,4). */
  private BigDecimal riskScore;

  /** Risk level. See {@link net.lab1024.sa.igaming.common.constant.RiskLevelEnum}. */
  private Integer riskLevel;

  /** Decision. See {@link net.lab1024.sa.igaming.common.constant.RiskDecisionEnum}. */
  private Integer decision;

  /** Rule execution results stored as JSONB. */
  private String ruleResultsJson;

  /** Processing time in milliseconds. */
  private Integer processingTimeMs;

  private OffsetDateTime createTime;
}
