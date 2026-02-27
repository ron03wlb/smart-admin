package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for GP debit callback (bet placement).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class CallbackDebitForm {

  @Schema(description = "Provider code")
  @NotBlank(message = "providerCode cannot be blank")
  @Size(max = 32)
  private String providerCode;

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Unique transaction ID (idempotency key)")
  @NotBlank(message = "transactionId cannot be blank")
  @Size(max = 128)
  private String transactionId;

  @Schema(description = "GP round ID")
  @NotBlank(message = "roundId cannot be blank")
  @Size(max = 128)
  private String roundId;

  @Schema(description = "Game code")
  @NotBlank(message = "gameCode cannot be blank")
  @Size(max = 64)
  private String gameCode;

  @Schema(description = "Bet amount")
  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.0001", message = "amount must be positive")
  @Digits(integer = 15, fraction = 4, message = "amount precision exceeded")
  private BigDecimal amount;

  @Schema(description = "GP signature")
  private String signature;

  @Schema(description = "Request timestamp (epoch millis)")
  private Long timestamp;
}
