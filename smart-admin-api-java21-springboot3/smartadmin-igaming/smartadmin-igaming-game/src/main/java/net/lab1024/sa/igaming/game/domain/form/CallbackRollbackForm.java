package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for GP rollback callback (bet cancellation).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class CallbackRollbackForm {

  @Schema(description = "Provider code")
  @NotBlank(message = "providerCode cannot be blank")
  @Size(max = 32)
  private String providerCode;

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Original transaction ID to reverse")
  @NotBlank(message = "originalTransactionId cannot be blank")
  @Size(max = 128)
  private String originalTransactionId;

  @Schema(description = "GP signature")
  private String signature;

  @Schema(description = "Request timestamp (epoch millis)")
  private Long timestamp;
}
