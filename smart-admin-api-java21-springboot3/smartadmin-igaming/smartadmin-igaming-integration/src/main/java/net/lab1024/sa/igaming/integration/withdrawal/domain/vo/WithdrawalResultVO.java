package net.lab1024.sa.igaming.integration.withdrawal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Withdrawal result VO — integrated withdrawal application result.
 *
 * <p>This VO includes both the payment order information and wallet balance information after the
 * withdrawal funds are locked.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class WithdrawalResultVO {

  @Schema(description = "Payment order number")
  private String orderNo;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Wallet ID")
  private Long walletId;

  @Schema(description = "Withdrawal amount")
  private BigDecimal amount;

  @Schema(description = "Currency code (ISO 4217)")
  private String currencyCode;

  @Schema(description = "Order status (see PaymentOrderStatusEnum)")
  private Integer status;

  @Schema(description = "Current wallet balance (after lock)")
  private BigDecimal currentBalance;

  @Schema(description = "Current locked amount (after lock)")
  private BigDecimal lockedAmount;

  @Schema(description = "Available balance (balance - lockedAmount)")
  private BigDecimal availableBalance;

  @Schema(description = "PSP code")
  private String pspCode;

  @Schema(description = "Risk check required flag")
  private Boolean riskCheckRequired;
}
