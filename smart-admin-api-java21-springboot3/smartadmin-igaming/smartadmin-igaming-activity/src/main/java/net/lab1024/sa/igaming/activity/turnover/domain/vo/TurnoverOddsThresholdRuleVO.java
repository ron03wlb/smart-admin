package net.lab1024.sa.igaming.activity.turnover.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Turnover odds threshold rule view object.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverOddsThresholdRuleVO {

  @Schema(description = "Rule ID")
  private Long ruleId;

  @Schema(description = "Rule code (unique per tenant)")
  private String ruleCode;

  @Schema(description = "Rule name (display name)")
  private String ruleName;

  @Schema(description = "Odds type (1-4)")
  private Integer oddsType;

  @Schema(description = "Threshold value (DECIMAL(10,4) precision)")
  private BigDecimal thresholdValue;

  @Schema(description = "Comparison operator (>=, >, <=, <, =)")
  private String comparisonOperator;

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
