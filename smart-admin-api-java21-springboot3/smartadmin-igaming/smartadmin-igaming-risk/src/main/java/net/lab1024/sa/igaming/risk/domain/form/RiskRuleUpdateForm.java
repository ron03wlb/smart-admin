package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Risk rule update form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskRuleUpdateForm {

  @Schema(description = "Rule param ID")
  @NotNull(message = "Rule ID is required")
  private Long ruleParamId;

  @Schema(description = "Rule name")
  private String ruleName;

  @Schema(description = "Rule description")
  private String ruleDescription;

  @Schema(description = "Threshold value")
  private BigDecimal thresholdValue;

  @Schema(description = "Time window in seconds")
  private Integer timeWindowSeconds;

  @Schema(description = "Max count")
  private Integer maxCount;

  @Schema(description = "Rule weight")
  private BigDecimal weight;

  @Schema(description = "Enabled")
  private Boolean enabled;

  @Schema(description = "Extra parameters (JSON)")
  private String paramsJson;
}
