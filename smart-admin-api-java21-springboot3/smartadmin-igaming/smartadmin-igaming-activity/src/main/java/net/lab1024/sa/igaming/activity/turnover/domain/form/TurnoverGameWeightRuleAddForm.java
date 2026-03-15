package net.lab1024.sa.igaming.activity.turnover.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Form for adding a turnover game weight rule.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverGameWeightRuleAddForm {

  @Schema(description = "Rule code (tenant-unique)", example = "GW_SLOTS_100")
  @NotBlank(message = "Rule code cannot be blank")
  @Size(max = 64)
  private String ruleCode;

  @Schema(description = "Rule name (display name)", example = "老虎機 100% 權重")
  @NotBlank(message = "Rule name cannot be blank")
  @Size(max = 128)
  private String ruleName;

  @Schema(description = "Game category (1-6)", example = "1")
  @NotNull(message = "Game category is required")
  private Integer gameCategory;

  @Schema(description = "Weight percentage (0.00 ~ 100.00)", example = "100.00")
  @NotNull(message = "Weight percentage is required")
  @DecimalMin(value = "0.00", message = "Weight percentage must be >= 0.00")
  @DecimalMax(value = "100.00", message = "Weight percentage must be <= 100.00")
  private BigDecimal weightPercentage;

  @Schema(description = "Priority (higher number = higher priority)", example = "100")
  @Positive(message = "Priority must be positive")
  private Integer priority;

  @Schema(description = "Effective from timestamp (nullable - applies immediately if null)")
  private OffsetDateTime effectiveFrom;

  @Schema(description = "Effective to timestamp (nullable - never expires if null)")
  private OffsetDateTime effectiveTo;

  @Schema(description = "Remark (business notes)")
  @Size(max = 512)
  private String remark;
}
