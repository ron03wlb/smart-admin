package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for freezing all wallets of a player.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class WalletFreezeForm {

  @Schema(description = "Player ID to freeze")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @Schema(description = "Freeze reason")
  @NotBlank(message = "reason cannot be blank")
  @Size(max = 256)
  private String reason;
}
