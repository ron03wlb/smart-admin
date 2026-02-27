package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.GameCategoryEnum;

/**
 * Form for adding a new game to the catalog.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameAddForm {

  @Schema(description = "Provider ID")
  @NotNull(message = "providerId cannot be null")
  private Long providerId;

  @Schema(description = "Game code")
  @NotBlank(message = "gameCode cannot be blank")
  @Size(max = 64)
  private String gameCode;

  @Schema(description = "Game name")
  @NotBlank(message = "gameName cannot be blank")
  @Size(max = 128)
  private String gameName;

  @SchemaEnum(GameCategoryEnum.class)
  @CheckEnum(message = "category invalid", value = GameCategoryEnum.class, required = true)
  private Integer category;

  @Schema(description = "Thumbnail URL")
  @Size(max = 512)
  private String thumbnailUrl;
}
