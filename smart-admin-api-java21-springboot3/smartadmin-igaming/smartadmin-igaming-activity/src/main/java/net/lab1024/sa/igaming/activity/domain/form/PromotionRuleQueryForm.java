package net.lab1024.sa.igaming.activity.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying promotion rules (paginated).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromotionRuleQueryForm extends PageParam {

  @Schema(description = "Keyword (name or code)")
  private String keyword;

  @Schema(description = "Promotion type filter (1-5)")
  private Integer promotionType;

  @Schema(description = "Status filter (1-3)")
  private Integer status;
}
