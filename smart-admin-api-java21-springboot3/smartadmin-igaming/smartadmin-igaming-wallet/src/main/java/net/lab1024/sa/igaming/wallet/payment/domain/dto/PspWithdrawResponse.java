package net.lab1024.sa.igaming.wallet.payment.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * PSP withdraw response DTO (internal, not exposed in API).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@Builder
public class PspWithdrawResponse {

  private String pspTransactionId;
  private String status;
}
