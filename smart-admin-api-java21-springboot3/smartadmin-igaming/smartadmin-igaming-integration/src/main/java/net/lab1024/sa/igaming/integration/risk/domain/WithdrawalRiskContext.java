package net.lab1024.sa.igaming.integration.risk.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * Withdrawal risk context — shared context for LiteFlow withdrawal risk assessment chain.
 *
 * <p>This context is passed through all LiteFlow components in the withdrawal risk check chain.
 * Each component reads input data, evaluates its specific risk rule, and writes its individual
 * score into {@code ruleScores}.
 *
 * <p><b>Risk Scoring:</b> Each component contributes a score (0-100). The final decision is based
 * on the weighted total score:
 *
 * <ul>
 *   <li>0-29: AUTO_APPROVED (low risk, proceed with withdrawal)
 *   <li>30-69: PENDING_REVIEW (medium/high risk, manual approval required)
 *   <li>70-100: AUTO_REJECTED (critical risk, block withdrawal)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class WithdrawalRiskContext {

  // ---- Input: Withdrawal metadata ----

  /** Payment order number. */
  private String orderNo;

  /** Player ID. */
  private Long playerId;

  /** Tenant ID (multi-tenant context). */
  private Long tenantId;

  /** Withdrawal amount. */
  private BigDecimal withdrawalAmount;

  /** Currency code (ISO 4217). */
  private String currencyCode;

  /** Current CASH wallet balance (before withdrawal). */
  private BigDecimal currentBalance;

  /** Locked amount (withdrawal funds are locked). */
  private BigDecimal lockedAmount;

  // ---- Input: Player risk profile ----

  /** KYC level (0=L0, 1=L1, 2=L2). */
  private Integer kycLevel;

  /** Player cumulative turnover (lifetime). */
  private BigDecimal cumulativeTurnover;

  /** Player total deposits (lifetime). */
  private BigDecimal totalDeposits;

  /** Player total withdrawals (lifetime). */
  private BigDecimal totalWithdrawals;

  // ---- Output: Risk assessment results ----

  /** Component ID -> individual score (0-100). */
  private Map<String, Integer> ruleScores = new HashMap<>();

  /**
   * Blocked flag — if true, withdrawal is automatically rejected.
   *
   * <p>Set by components that detect critical violations (e.g., KYC L0, AML insufficient turnover).
   */
  private boolean blocked = false;

  /** Reason for blocking (if blocked=true). */
  private String blockReason;

  /**
   * Flagged for manual review — if true, withdrawal requires manual approval.
   *
   * <p>Set by components that detect medium-risk conditions (e.g., KYC L1 exceeds limit, high
   * velocity).
   */
  private boolean flagged = false;

  /** Reason for flagging (if flagged=true). */
  private String flagReason;

  /**
   * Add a component's individual risk score.
   *
   * @param componentId LiteFlow component ID
   * @param score score value (0-100)
   */
  public void addRuleScore(String componentId, int score) {
    ruleScores.put(componentId, Math.max(0, Math.min(100, score)));
  }

  /**
   * Calculate the weighted total score from all component scores.
   *
   * @param weights component ID -> weight mapping (e.g., {"kycLevelCheck": 3.0, "amlTurnoverCheck":
   *     2.0, "velocityCheck": 1.0})
   * @return weighted total score (0-100)
   */
  public int calculateTotalScore(Map<String, BigDecimal> weights) {
    BigDecimal totalWeightedScore = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;

    for (Map.Entry<String, Integer> entry : ruleScores.entrySet()) {
      BigDecimal weight = weights.getOrDefault(entry.getKey(), BigDecimal.ONE);
      totalWeightedScore =
          totalWeightedScore.add(BigDecimal.valueOf(entry.getValue()).multiply(weight));
      totalWeight = totalWeight.add(weight);
    }

    if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }

    return totalWeightedScore.divide(totalWeight, 0, java.math.RoundingMode.HALF_UP).intValue();
  }

  /**
   * Get final risk level based on total score.
   *
   * @param totalScore weighted total score (0-100)
   * @return risk level: LOW, MEDIUM, HIGH, CRITICAL
   */
  public String getRiskLevel(int totalScore) {
    if (totalScore >= 70) {
      return "CRITICAL";
    } else if (totalScore >= 50) {
      return "HIGH";
    } else if (totalScore >= 30) {
      return "MEDIUM";
    }
    return "LOW";
  }

  /**
   * Get final decision based on total score and flags.
   *
   * @param totalScore weighted total score (0-100)
   * @return decision: AUTO_APPROVED, PENDING_REVIEW, AUTO_REJECTED
   */
  public String getDecision(int totalScore) {
    if (blocked || totalScore >= 70) {
      return "AUTO_REJECTED";
    } else if (flagged || totalScore >= 30) {
      return "PENDING_REVIEW";
    }
    return "AUTO_APPROVED";
  }
}
