package net.lab1024.sa.igaming.activity.turnover.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Form for updating a turnover risk action rule.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverRiskActionRuleUpdateForm {

  @Schema(description = "Rule ID", example = "1")
  @NotNull(message = "Rule ID is required")
  private Long ruleId;

  @Schema(description = "Rule code (tenant-unique)", example = "RA_PASS")
  @NotBlank(message = "Rule code cannot be blank")
  @Size(max = 64)
  private String ruleCode;

  @Schema(description = "Rule name (display name)", example = "通過 - 正常計算流水")
  @NotBlank(message = "Rule name cannot be blank")
  @Size(max = 128)
  private String ruleName;

  @Schema(description = "Risk level (1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL)", example = "1")
  @NotNull(message = "Risk level is required")
  private Integer riskLevel;

  @Schema(description = "Action type (1=PASS, 2=FLAG, 3=BLOCK)", example = "1")
  @NotNull(message = "Action type is required")
  private Integer actionType;

  @Schema(description = "Turnover factor (0.00 ~ 100.00)", example = "100.00")
  @NotNull(message = "Turnover factor is required")
  @DecimalMin(value = "0.00", message = "Turnover factor must be >= 0.00")
  @DecimalMax(value = "100.00", message = "Turnover factor must be <= 100.00")
  private BigDecimal turnoverFactor;

  @Schema(description = "Allow bet flag (true = allow, false = block)", example = "true")
  @NotNull(message = "Allow bet flag is required")
  private Boolean allowBet;

  @Schema(
      description = "Create risk proposal flag (true = create, false = no action)",
      example = "false")
  @NotNull(message = "Create proposal flag is required")
  private Boolean createProposal;

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

  @Schema(description = "Change reason (required for auditing)", example = "調整風控策略")
  @NotBlank(message = "Change reason is required for update operations")
  @Size(max = 512)
  private String changeReason;
}
