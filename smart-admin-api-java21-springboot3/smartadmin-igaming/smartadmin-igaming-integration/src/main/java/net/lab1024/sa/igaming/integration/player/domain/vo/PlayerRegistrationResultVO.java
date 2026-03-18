package net.lab1024.sa.igaming.integration.player.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;

/**
 * Result VO for integrated player registration.
 *
 * <p>This VO combines player authentication details with wallet information, providing a complete
 * view of the newly registered player's account setup.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class PlayerRegistrationResultVO {

  @Schema(description = "Player authentication details (ID, username, token, VIP level)")
  private PlayerAuthVO player;

  @Schema(description = "Created wallets (CASH + BONUS)")
  private List<WalletVO> wallets;

  @Schema(description = "Whether eligible for first deposit bonus")
  private Boolean firstDepositBonusEligible;

  @Schema(description = "Referral code used (if applicable)")
  private String referralCode;
}
