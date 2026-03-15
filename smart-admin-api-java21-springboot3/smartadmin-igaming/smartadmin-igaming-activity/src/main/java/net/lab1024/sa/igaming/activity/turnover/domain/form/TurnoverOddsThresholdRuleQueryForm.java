package net.lab1024.sa.igaming.activity.turnover.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying turnover odds threshold rules (paginated).
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TurnoverOddsThresholdRuleQueryForm extends PageParam {

  @Schema(description = "Keyword (rule name or code)")
  private String keyword;

  @Schema(description = "Odds type filter (1=EUR, 2=HK, 3=MY, 4=ID)")
  private Integer oddsType;

  @Schema(description = "Status filter (1=ENABLED, 2=DISABLED, 3=EXPIRED)")
  private Integer status;
}
