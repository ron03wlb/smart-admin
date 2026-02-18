package net.lab1024.sa.igaming.wallet.payment.domain.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * PSP query response DTO — used for reconciliation status checks.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@Builder
public class PspQueryResponse {

  private String pspTransactionId;
  private String status;
  private BigDecimal amount;
}
