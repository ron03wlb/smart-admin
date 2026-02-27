package net.lab1024.sa.igaming.activity.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying wagering progress records (paginated).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WageringProgressQueryForm extends PageParam {

  @Schema(description = "Player ID filter")
  private Long playerId;

  @Schema(description = "Bonus record status filter (1-5)")
  private Integer status;
}
