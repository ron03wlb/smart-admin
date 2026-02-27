package net.lab1024.sa.igaming.risk.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Risk rule parameter view object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskRuleVO {

  @Schema(description = "Rule param ID")
  private Long ruleParamId;

  @Schema(description = "Rule type")
  private Integer ruleType;

  @Schema(description = "Rule name")
  private String ruleName;

  @Schema(description = "Rule description")
  private String ruleDescription;

  @Schema(description = "Threshold value")
  private BigDecimal thresholdValue;

  @Schema(description = "Time window (seconds)")
  private Integer timeWindowSeconds;

  @Schema(description = "Max count")
  private Integer maxCount;

  @Schema(description = "Weight")
  private BigDecimal weight;

  @Schema(description = "Enabled")
  private Boolean enabled;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;

  @Schema(description = "Update time")
  private OffsetDateTime updateTime;
}
