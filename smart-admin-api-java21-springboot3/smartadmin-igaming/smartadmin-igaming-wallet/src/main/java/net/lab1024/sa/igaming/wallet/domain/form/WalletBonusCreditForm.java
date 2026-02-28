package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Form for crediting bonus funds into a BONUS wallet.
 *
 * <p>Creates a bonus extension record alongside the wallet credit, tracking wagering requirements
 * and expiration.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Data
public class WalletBonusCreditForm {

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Bonus amount")
  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.0001", message = "amount must be positive")
  @DecimalMax(value = "9999999999.9999", message = "amount exceeds maximum limit")
  @Digits(integer = 15, fraction = 4, message = "amount precision exceeded (max 4 decimal places)")
  private BigDecimal amount;

  @Schema(description = "Bonus record ID (from activity module)")
  @NotNull(message = "bonusId cannot be null")
  private Long bonusId;

  @Schema(description = "Wagering requirement to unlock bonus")
  @NotNull(message = "wageringRequirement cannot be null")
  @DecimalMin(value = "0", message = "wageringRequirement cannot be negative")
  private BigDecimal wageringRequirement;

  @Schema(description = "Bonus expiration time")
  @NotNull(message = "expiresAt cannot be null")
  private OffsetDateTime expiresAt;

  @Schema(description = "Game restriction rules (JSON)")
  @Size(max = 2000)
  private String gameRestriction;

  @Schema(description = "Idempotency key")
  @NotBlank(message = "requestId cannot be blank")
  @Size(max = 64)
  private String requestId;

  @Schema(description = "Transaction description")
  @Size(max = 200)
  private String description;
}
