package net.lab1024.sa.igaming.integration.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Form for game launch request.
 *
 * <p>This form captures the player's request to launch a game, including game selection and
 * optional lobby parameters.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class GameLaunchForm {

  @Schema(description = "Player ID (from Sa-Token context)")
  @NotNull(message = "Player ID cannot be null")
  private Long playerId;

  @Schema(description = "Game code (e.g., 'slot-001', 'baccarat-live')")
  @NotBlank(message = "Game code cannot be blank")
  private String gameCode;

  @Schema(description = "Game provider code (e.g., 'pragmatic-play', 'evolution')")
  @NotBlank(message = "Provider code cannot be blank")
  private String providerCode;

  @Schema(
      description = "Lobby URL to return after game exit (optional, defaults to platform lobby)")
  private String returnUrl;

  @Schema(description = "Language code (e.g., 'en', 'zh-CN', default: 'en')")
  private String language = "en";

  @Schema(description = "Currency code (e.g., 'USD', 'CNY', defaults to player's currency)")
  private String currencyCode;
}
