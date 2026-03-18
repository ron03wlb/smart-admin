package net.lab1024.sa.igaming.integration.payment.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Result VO for integrated deposit processing with bonus.
 *
 * <p>This VO extends standard deposit confirmation with first deposit bonus information, providing
 * a complete view of the deposit outcome including any promotional credits awarded.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class DepositResultVO {

  @Schema(description = "Platform order number")
  private String orderNo;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Wallet ID (CASH wallet)")
  private Long walletId;

  @Schema(description = "Deposit amount credited to CASH wallet")
  private BigDecimal depositAmount;

  @Schema(description = "CASH wallet balance after deposit")
  private BigDecimal cashBalance;

  @Schema(description = "Whether this is the player's first deposit")
  private Boolean isFirstDeposit;

  @Schema(description = "First deposit bonus amount (0 if not applicable)")
  private BigDecimal bonusAmount;

  @Schema(description = "BONUS wallet ID (if bonus awarded)")
  private Long bonusWalletId;

  @Schema(description = "BONUS wallet balance after bonus credit")
  private BigDecimal bonusBalance;

  @Schema(description = "Promotion code that triggered the bonus (if applicable)")
  private String promotionCode;
}
