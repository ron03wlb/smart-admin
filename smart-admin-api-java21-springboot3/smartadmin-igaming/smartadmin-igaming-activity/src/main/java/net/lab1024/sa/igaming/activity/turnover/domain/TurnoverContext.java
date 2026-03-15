package net.lab1024.sa.igaming.activity.turnover.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Turnover calculation context passed through LiteFlow chain.
 *
 * <p>Encapsulates bet settlement data and intermediate calculation results across three validation
 * layers:
 *
 * <ul>
 *   <li>Layer 1: Risk filtering (odds threshold, risk action)
 *   <li>Layer 2: Settlement status factor (win/loss/draw/void)
 *   <li>Layer 3: Game weight (category-specific multiplier)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Data
public class TurnoverContext {

  // ---- Input: bet settlement data ----

  /** Bet ID (unique identifier) */
  private String betId;

  /** Player ID */
  private Long playerId;

  /** Tenant ID (from multi-tenant context) */
  private Long tenantId;

  /** Bet amount (original wager) */
  private BigDecimal betAmount;

  /** Game category (1-6: Slots, Live Casino, Sports Betting, Poker, Table Games, Lottery) */
  private Integer gameCategory;

  /** Settlement status (1-9: WIN, LOSS, DRAW, TIE, VOID, CANCEL, HALF_WIN, HALF_LOSS, RUNNING) */
  private Integer settlementStatus;

  /** Odds value (e.g., 1.95 for sports betting, 0.98 for baccarat) */
  private BigDecimal oddsValue;

  /** Odds type (1-4: Decimal, Fractional, American, Hong Kong) */
  private Integer oddsType;

  /** Risk score from risk assessment system (0-100) */
  private Integer riskScore;

  // ---- Intermediate results (populated by LiteFlow nodes) ----

  /** Risk action type determined by Layer 1 (1: PASS, 2: FLAG, 3: BLOCK, 4: MANUAL_REVIEW) */
  private Integer riskActionType;

  /** Turnover factor percentage (0.00 - 100.00) from risk action rule */
  private BigDecimal turnoverFactor;

  /** Rejected flag (true if bet is blocked by risk filter) */
  private Boolean rejected = false;

  /** Status factor percentage (0.00 - 100.00) based on settlement status */
  private BigDecimal statusFactor;

  /** Game weight percentage (0.00 - 100.00) based on game category */
  private BigDecimal gameWeight;

  // ---- Final results ----

  /**
   * Effective turnover base = betAmount * turnoverFactor
   *
   * <p>After Layer 1 risk filtering.
   */
  private BigDecimal effectiveTurnoverBase;

  /**
   * Valid turnover for finance = effectiveTurnoverBase * statusFactor
   *
   * <p>After Layer 2 settlement status adjustment.
   */
  private BigDecimal validTurnoverFinance;

  /**
   * Activity valid turnover = validTurnoverFinance * gameWeight
   *
   * <p>Final result after Layer 3 game weight application. This is the amount used for bonus
   * wagering requirements.
   */
  private BigDecimal activityValidTurnover;

  // ---- Audit information ----

  /** Reason for rejection (if rejected = true) */
  private String rejectedBy;

  /** List of matched rule codes for audit trail */
  private List<String> matchedRules = new ArrayList<>();

  /** Calculation timestamp (UTC) */
  private OffsetDateTime calculatedAt = OffsetDateTime.now(ZoneOffset.UTC);

  /**
   * Record a matched rule for audit purposes.
   *
   * @param ruleCode rule code that was applied
   */
  public void addMatchedRule(String ruleCode) {
    this.matchedRules.add(ruleCode);
  }

  /**
   * Mark this calculation as rejected due to risk filter.
   *
   * @param reason rejection reason (e.g., "ODDS_TOO_LOW", "RISK_SCORE_HIGH")
   */
  public void markRejected(String reason) {
    this.rejected = true;
    this.rejectedBy = reason;
    this.turnoverFactor = BigDecimal.ZERO;
    this.effectiveTurnoverBase = BigDecimal.ZERO;
    this.validTurnoverFinance = BigDecimal.ZERO;
    this.activityValidTurnover = BigDecimal.ZERO;
  }

  /**
   * Check if calculation was rejected.
   *
   * @return true if rejected by risk filter
   */
  public boolean isRejected() {
    return Boolean.TRUE.equals(rejected);
  }
}
