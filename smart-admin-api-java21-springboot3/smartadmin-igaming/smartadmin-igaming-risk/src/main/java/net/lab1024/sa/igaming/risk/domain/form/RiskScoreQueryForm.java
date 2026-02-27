package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Risk score query form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskScoreQueryForm extends PageParam {

  @Schema(description = "Risk level filter")
  private Integer riskLevel;

  @Schema(description = "Auto-locked filter")
  private Boolean autoLocked;

  @Schema(description = "Player ID filter")
  private Long playerId;
}
