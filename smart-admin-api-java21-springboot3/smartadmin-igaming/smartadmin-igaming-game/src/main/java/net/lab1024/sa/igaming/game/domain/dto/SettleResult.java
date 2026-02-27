package net.lab1024.sa.igaming.game.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Settle result DTO from GP adapter.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class SettleResult {

  private String transactionRef;
  private String roundId;
  private BigDecimal payoutAmount;
  private String status;
}
