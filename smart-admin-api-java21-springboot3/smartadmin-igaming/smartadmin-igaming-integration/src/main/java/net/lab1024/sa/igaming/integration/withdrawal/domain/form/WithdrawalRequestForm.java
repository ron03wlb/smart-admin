package net.lab1024.sa.igaming.integration.withdrawal.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Withdrawal request form — player-facing simplified withdrawal form.
 *
 * <p>This is a high-level integration form that accepts playerId and amount. The integration
 * service will resolve the walletId, check available balance, and delegate to the Wallet module's
 * PaymentService.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class WithdrawalRequestForm {

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Withdrawal amount")
  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.0001", message = "amount must be positive")
  @DecimalMax(value = "9999999999.9999", message = "amount exceeds maximum limit")
  @Digits(integer = 15, fraction = 4, message = "amount precision exceeded (max 4 decimal places)")
  private BigDecimal amount;

  @Schema(description = "Idempotency key")
  @NotBlank(message = "requestId cannot be blank")
  @Size(max = 64)
  private String requestId;

  @Schema(description = "Description (optional)")
  @Size(max = 200)
  private String description;
}
