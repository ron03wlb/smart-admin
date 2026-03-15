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
 * Form for updating a turnover status factor rule.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverStatusFactorRuleUpdateForm {

  @Schema(description = "Rule ID", example = "1")
  @NotNull(message = "Rule ID is required")
  private Long ruleId;

  @Schema(description = "Rule code (tenant-unique)", example = "SF_WIN_100")
  @NotBlank(message = "Rule code cannot be blank")
  @Size(max = 64)
  private String ruleCode;

  @Schema(description = "Rule name (display name)", example = "贏局 100% 流水")
  @NotBlank(message = "Rule name cannot be blank")
  @Size(max = 128)
  private String ruleName;

  @Schema(description = "Settlement status (1-9)", example = "1")
  @NotNull(message = "Settlement status is required")
  private Integer settlementStatus;

  @Schema(description = "Factor percentage (0.00 ~ 100.00)", example = "100.00")
  @NotNull(message = "Factor percentage is required")
  @DecimalMin(value = "0.00", message = "Factor percentage must be >= 0.00")
  @DecimalMax(value = "100.00", message = "Factor percentage must be <= 100.00")
  private BigDecimal factorPercentage;

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

  @Schema(description = "Change reason (required for auditing)", example = "調整平局計算規則")
  @NotBlank(message = "Change reason is required for update operations")
  @Size(max = 512)
  private String changeReason;
}
