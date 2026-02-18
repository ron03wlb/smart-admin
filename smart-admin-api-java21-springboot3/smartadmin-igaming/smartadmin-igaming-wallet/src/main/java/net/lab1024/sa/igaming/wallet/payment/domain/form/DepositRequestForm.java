package net.lab1024.sa.igaming.wallet.payment.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for creating a deposit order.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class DepositRequestForm {

  @Schema(description = "Wallet ID")
  @NotNull(message = "walletId cannot be null")
  private Long walletId;

  @Schema(description = "Deposit amount")
  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.0001", message = "amount must be positive")
  @DecimalMax(value = "9999999999.9999", message = "amount exceeds maximum limit")
  @Digits(integer = 15, fraction = 4, message = "amount precision exceeded (max 4 decimal places)")
  private BigDecimal amount;

  @Schema(description = "Currency code (ISO 4217)")
  @Size(min = 3, max = 3, message = "currencyCode must be 3 characters")
  @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be uppercase ISO 4217 format")
  private String currencyCode;

  @Schema(description = "Payment method (e.g., credit_card, bank_transfer)")
  @Size(max = 30)
  private String paymentMethod;

  @Schema(description = "PSP code (optional, auto-routed if not specified)")
  @Size(max = 30)
  private String pspCode;

  @Schema(description = "Idempotency key")
  @NotBlank(message = "requestId cannot be blank")
  @Size(max = 64)
  private String requestId;

  @Schema(description = "Description")
  @Size(max = 200)
  private String description;
}
