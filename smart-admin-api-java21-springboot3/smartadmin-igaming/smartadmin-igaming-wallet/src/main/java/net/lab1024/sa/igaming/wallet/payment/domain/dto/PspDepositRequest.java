package net.lab1024.sa.igaming.wallet.payment.domain.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * PSP deposit request DTO (internal, not exposed in API).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@Builder
public class PspDepositRequest {

  private String orderId;
  private BigDecimal amount;
  private String currency;
  private String callbackUrl;
  private String returnUrl;
  private String paymentMethod;
}
