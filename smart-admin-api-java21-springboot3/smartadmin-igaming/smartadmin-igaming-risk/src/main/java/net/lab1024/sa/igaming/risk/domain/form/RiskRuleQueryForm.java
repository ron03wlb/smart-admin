package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Risk rule query form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskRuleQueryForm extends PageParam {

  @Schema(description = "Rule type filter")
  private Integer ruleType;

  @Schema(description = "Keyword (name search)")
  private String keyword;

  @Schema(description = "Enabled filter")
  private Boolean enabled;
}
