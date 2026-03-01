package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for GP balance query callback.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class CallbackBalanceForm {

  @Schema(description = "Provider code")
  @NotBlank(message = "providerCode cannot be blank")
  @Size(max = 32)
  private String providerCode;

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Game code (optional — used for bonus eligibility check)")
  @Size(max = 64)
  private String gameCode;

  @Schema(description = "GP signature")
  private String signature;

  @Schema(description = "Request timestamp (epoch millis)")
  private Long timestamp;
}
