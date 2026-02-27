package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.RiskRuleTypeEnum;

/**
 * Risk rule add form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskRuleAddForm {

  @Schema(description = "Rule type")
  @NotNull(message = "Rule type is required")
  @CheckEnum(value = RiskRuleTypeEnum.class, message = "Invalid rule type")
  private Integer ruleType;

  @Schema(description = "Rule name")
  @NotBlank(message = "Rule name is required")
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
  @NotNull(message = "Weight is required")
  private BigDecimal weight;

  @Schema(description = "Extra parameters (JSON)")
  private String paramsJson;
}
