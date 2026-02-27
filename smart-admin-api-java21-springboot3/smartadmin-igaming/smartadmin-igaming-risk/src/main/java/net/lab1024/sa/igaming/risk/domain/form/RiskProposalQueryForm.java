package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Risk proposal query form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskProposalQueryForm extends PageParam {

  @Schema(description = "Status filter")
  private Integer status;

  @Schema(description = "Priority filter")
  private Integer priority;

  @Schema(description = "Player ID filter")
  private Long playerId;
}
