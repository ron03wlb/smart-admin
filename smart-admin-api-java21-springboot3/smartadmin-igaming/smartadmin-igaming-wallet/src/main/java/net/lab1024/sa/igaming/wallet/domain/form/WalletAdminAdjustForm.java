package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Form for admin wallet balance adjustment.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class WalletAdminAdjustForm {

  @Schema(description = "Wallet ID to adjust")
  @NotNull(message = "walletId cannot be null")
  private Long walletId;

  @Schema(description = "Adjustment amount (positive=credit, negative=debit)")
  @NotNull(message = "amount cannot be null")
  private BigDecimal amount;

  @Schema(description = "Idempotency key for adjustment")
  @NotBlank(message = "requestId cannot be blank")
  @Size(max = 128)
  private String requestId;

  @Schema(description = "Audit reason for adjustment")
  @NotBlank(message = "reason cannot be blank")
  @Size(max = 256)
  private String reason;
}
