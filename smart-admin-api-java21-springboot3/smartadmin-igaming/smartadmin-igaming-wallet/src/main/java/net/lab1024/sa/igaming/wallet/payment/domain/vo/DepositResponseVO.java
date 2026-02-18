package net.lab1024.sa.igaming.wallet.payment.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Deposit response VO — returned after deposit order creation.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class DepositResponseVO {

  @Schema(description = "Platform order number")
  private String orderNo;

  @Schema(description = "PSP redirect URL for payment")
  private String redirectUrl;

  @Schema(description = "Order status")
  private Integer status;
}
