package net.lab1024.sa.igaming.activity.turnover.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Form for updating a turnover odds threshold rule.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverOddsThresholdRuleUpdateForm {

  @Schema(description = "Rule ID", example = "1")
  @NotNull(message = "Rule ID is required")
  private Long ruleId;

  @Schema(description = "Rule code (tenant-unique)", example = "OT_EUR_1_5")
  @NotBlank(message = "Rule code cannot be blank")
  @Size(max = 64)
  private String ruleCode;

  @Schema(description = "Rule name (display name)", example = "歐洲盤 >= 1.5")
  @NotBlank(message = "Rule name cannot be blank")
  @Size(max = 128)
  private String ruleName;

  @Schema(description = "Odds type (1=EUR, 2=HK, 3=MY, 4=ID)", example = "1")
  @NotNull(message = "Odds type is required")
  private Integer oddsType;

  @Schema(description = "Threshold value", example = "1.5000")
  @NotNull(message = "Threshold value is required")
  @DecimalMin(value = "0.0001", message = "Threshold value must be > 0")
  private BigDecimal thresholdValue;

  @Schema(description = "Comparison operator ('>=', '>', '<=', '<', '=')", example = ">=")
  @NotBlank(message = "Comparison operator is required")
  @Pattern(
      regexp = "^(>=|>|<=|<|=)$",
      message = "Comparison operator must be one of: >=, >, <=, <, =")
  @Size(max = 2)
  private String comparisonOperator;

  @Schema(description = "Effective from timestamp (nullable - applies immediately if null)")
  private OffsetDateTime effectiveFrom;

  @Schema(description = "Effective to timestamp (nullable - never expires if null)")
  private OffsetDateTime effectiveTo;

  @Schema(description = "Status (1=ENABLED, 2=DISABLED, 3=EXPIRED)", example = "1")
  @NotNull(message = "Status is required")
  private Integer status;

  @Schema(description = "Remark (business notes)")
  @Size(max = 512)
  private String remark;

  @Schema(description = "Change reason (required for auditing)", example = "調整賠率閾值防止套利")
  @NotBlank(message = "Change reason is required for update operations")
  @Size(max = 512)
  private String changeReason;
}
