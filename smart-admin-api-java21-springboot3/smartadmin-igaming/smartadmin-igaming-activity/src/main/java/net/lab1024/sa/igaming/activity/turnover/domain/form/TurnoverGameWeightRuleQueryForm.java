package net.lab1024.sa.igaming.activity.turnover.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying turnover game weight rules (paginated).
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TurnoverGameWeightRuleQueryForm extends PageParam {

  @Schema(description = "Keyword (rule name or code)")
  private String keyword;

  @Schema(description = "Game category filter (1-6)")
  private Integer gameCategory;

  @Schema(description = "Status filter (1=ENABLED, 2=DISABLED, 3=EXPIRED)")
  private Integer status;
}
