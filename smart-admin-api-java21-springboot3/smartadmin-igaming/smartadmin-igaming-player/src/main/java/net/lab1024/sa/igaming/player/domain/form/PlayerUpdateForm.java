package net.lab1024.sa.igaming.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for updating player profile.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerUpdateForm {

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Email address (optional)")
  @Size(max = 200)
  private String email;

  @Schema(description = "Phone number (optional)")
  @Size(max = 30)
  private String phone;
}
