package net.lab1024.sa.igaming.game.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Standardized bet request DTO for GP adapter.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class BetRequest {

  private Long playerId;
  private Long tenantId;
  private String roundId;
  private String gameCode;
  private BigDecimal amount;
  private String transactionId;
}
