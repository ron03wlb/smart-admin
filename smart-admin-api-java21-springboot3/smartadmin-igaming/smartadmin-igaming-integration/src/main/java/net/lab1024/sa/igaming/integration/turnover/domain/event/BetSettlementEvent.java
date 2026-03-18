package net.lab1024.sa.igaming.integration.turnover.domain.event;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Bet settlement event - published by Game module when a bet is settled.
 *
 * <p>This event triggers turnover calculation and wagering progress update in the Activity module.
 * The integration service consumes this event via Kafka and orchestrates the turnover flow.
 *
 * <p><b>Event Flow:</b>
 *
 * <ol>
 *   <li>Game module settles bet (WIN/LOSS/DRAW) → publishes ROUND_SETTLED event
 *   <li>TurnoverEventConsumer receives event → extracts bet details
 *   <li>TurnoverIntegrationService.processBetSettlement() → calculates turnover
 *   <li>WageringProgressManager.updateWageringProgress() → updates bonus progress
 * </ol>
 *
 * <p><b>Kafka Topic:</b> {@code game-events}
 *
 * <p><b>Event Type:</b> {@code ROUND_SETTLED}
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class BetSettlementEvent {

  /** Game round ID (t_game_round.round_id). */
  private Long roundId;

  /** Player ID (t_player.player_id). */
  private Long playerId;

  /** Tenant ID (multi-tenant context). */
  private Long tenantId;

  /** Bet ID (unique transaction identifier). */
  private String betId;

  /** Game code (e.g., "slot-001", "baccarat-live"). */
  private String gameCode;

  /** Game provider code (e.g., "pragmatic-play", "evolution"). */
  private String providerCode;

  /** Game category (1: Slots, 2: Live Casino, 3: Sports, 4: Poker, 5: Table Games, 6: Lottery). */
  private Integer gameCategory;

  /** Bet amount (original wager). */
  private BigDecimal betAmount;

  /** Payout amount (winnings). */
  private BigDecimal payoutAmount;

  /**
   * Settlement status.
   *
   * <ul>
   *   <li>1: WIN
   *   <li>2: LOSS
   *   <li>3: DRAW
   *   <li>4: TIE
   *   <li>5: VOID
   *   <li>6: CANCEL
   *   <li>7: HALF_WIN
   *   <li>8: HALF_LOSS
   *   <li>9: RUNNING
   * </ul>
   */
  private Integer settlementStatus;

  /**
   * Odds value (e.g., 1.95 for decimal odds).
   *
   * <p>Used for odds threshold validation in turnover calculation (Layer 1 - Risk Filter).
   */
  private BigDecimal oddsValue;

  /**
   * Odds type.
   *
   * <ul>
   *   <li>1: DECIMAL (European, e.g., 1.95)
   *   <li>2: FRACTIONAL (UK, e.g., 19/20)
   *   <li>3: AMERICAN (US, e.g., -105)
   *   <li>4: HONG_KONG (HK, e.g., 0.95)
   * </ul>
   */
  private Integer oddsType;

  /**
   * Risk score (0-100, optional).
   *
   * <p>Risk score from risk module (if available). Used for risk-based turnover adjustments.
   *
   * <ul>
   *   <li>0-29: LOW (risk level 1) → 100% turnover
   *   <li>30-49: MEDIUM (risk level 2) → 100% turnover + FLAG for review
   *   <li>50-69: HIGH (risk level 3) → 100% turnover + FLAG for review
   *   <li>70-100: CRITICAL (risk level 4) → BLOCK (0% turnover)
   * </ul>
   */
  private Integer riskScore;

  /** Event timestamp (ISO-8601 format). */
  private String settledAt;
}
