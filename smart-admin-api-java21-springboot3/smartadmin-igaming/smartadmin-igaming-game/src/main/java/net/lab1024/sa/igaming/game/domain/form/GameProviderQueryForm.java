package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying game providers (paginated).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GameProviderQueryForm extends PageParam {

  @Schema(description = "Provider code filter")
  private String providerCode;

  @Schema(description = "Health status filter (1=HEALTHY, 2=DEGRADED, 3=DOWN)")
  private Integer healthStatus;

  @Schema(description = "Enabled filter")
  private Boolean enabled;
}
