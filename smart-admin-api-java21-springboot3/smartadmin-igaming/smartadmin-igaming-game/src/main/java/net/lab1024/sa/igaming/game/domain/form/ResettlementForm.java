package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for admin resettlement of a game round.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class ResettlementForm {

  @Schema(description = "Round ID to resettle")
  @NotNull(message = "roundId cannot be null")
  private Long roundId;

  @Schema(description = "Corrected payout amount")
  @NotNull(message = "newPayoutAmount cannot be null")
  @DecimalMin(value = "0.00", message = "newPayoutAmount must be >= 0")
  private BigDecimal newPayoutAmount;

  @Schema(description = "Idempotency key for resettlement")
  @NotBlank(message = "requestId cannot be blank")
  @Size(max = 128)
  private String requestId;
}
