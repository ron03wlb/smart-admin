package net.lab1024.sa.igaming.risk.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for resetting auto-lock on a player's risk score profile.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class RiskResetAutoLockForm {

  @Schema(description = "Player ID to unlock")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Unlock reason")
  @NotBlank(message = "reason cannot be blank")
  @Size(max = 256)
  private String reason;
}
