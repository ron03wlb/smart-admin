package net.lab1024.sa.igaming.activity.turnover.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Turnover risk action rule view object.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverRiskActionRuleVO {

  @Schema(description = "Rule ID")
  private Long ruleId;

  @Schema(description = "Rule code (unique per tenant)")
  private String ruleCode;

  @Schema(description = "Rule name (display name)")
  private String ruleName;

  @Schema(description = "Risk level (1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL)")
  private Integer riskLevel;

  @Schema(description = "Action type (1-3)")
  private Integer actionType;

  @Schema(description = "Turnover factor (0.00 ~ 100.00)")
  private BigDecimal turnoverFactor;

  @Schema(description = "Allow bet flag (true = allow, false = block)")
  private Boolean allowBet;

  @Schema(description = "Create risk proposal flag (true = create, false = no action)")
  private Boolean createProposal;

  @Schema(description = "Effective from timestamp")
  private OffsetDateTime effectiveFrom;

  @Schema(description = "Effective to timestamp")
  private OffsetDateTime effectiveTo;

  @Schema(description = "Status (1=Enabled, 2=Disabled, 3=Expired)")
  private Integer status;

  @Schema(description = "Remark (business notes)")
  private String remark;

  @Schema(description = "Soft delete flag")
  private Boolean deleted;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;

  @Schema(description = "Update time")
  private OffsetDateTime updateTime;
}
