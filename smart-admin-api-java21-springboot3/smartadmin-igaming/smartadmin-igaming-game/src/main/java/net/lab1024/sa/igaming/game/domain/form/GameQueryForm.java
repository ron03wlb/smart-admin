package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * Form for querying games (paginated).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GameQueryForm extends PageParam {

  @Schema(description = "Keyword (game name or code)")
  private String keyword;

  @Schema(description = "Category filter (1-6)")
  private Integer category;

  @Schema(description = "Provider code filter")
  private String providerCode;

  @Schema(description = "Enabled filter")
  private Boolean enabled;
}
