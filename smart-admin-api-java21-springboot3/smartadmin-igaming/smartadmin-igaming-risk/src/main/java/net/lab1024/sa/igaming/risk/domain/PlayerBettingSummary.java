package net.lab1024.sa.igaming.risk.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Aggregated betting behavior summary for a player within a time window.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerBettingSummary {

  private Long playerId;

  /** Number of bets within the time window. */
  private int betCount;

  /** Total amount wagered within the time window. */
  private BigDecimal totalWagered;

  /** Average single bet amount. */
  private BigDecimal averageBet;

  /** Maximum single bet amount. */
  private BigDecimal maxBet;

  /** Win rate (0.0 - 1.0). */
  private BigDecimal winRate;

  /** All-in ratio (bets equal to balance / total bets). */
  private BigDecimal allInRatio;
}
