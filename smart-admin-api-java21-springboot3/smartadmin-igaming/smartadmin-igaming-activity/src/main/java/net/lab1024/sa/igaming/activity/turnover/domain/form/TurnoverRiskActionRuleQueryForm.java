package net.lab1024.sa.igaming.activity.turnover.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying turnover risk action rules (paginated).
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TurnoverRiskActionRuleQueryForm extends PageParam {

  @Schema(description = "Keyword (rule name or code)")
  private String keyword;

  @Schema(description = "Risk level filter (1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL)")
  private Integer riskLevel;

  @Schema(description = "Action type filter (1=PASS, 2=FLAG, 3=BLOCK)")
  private Integer actionType;

  @Schema(description = "Status filter (1=ENABLED, 2=DISABLED, 3=EXPIRED)")
  private Integer status;
}
