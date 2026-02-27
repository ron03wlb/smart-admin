package net.lab1024.sa.igaming.game.domain.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Game round detail DTO from GP query.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameRoundDetail {

  private String roundId;
  private String status;
  private BigDecimal betAmount;
  private BigDecimal payoutAmount;
}
