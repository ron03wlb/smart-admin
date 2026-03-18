package net.lab1024.sa.igaming.integration.risk.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.integration.risk.domain.WithdrawalRiskContext;

/**
 * AML turnover check component — validates minimum turnover requirement for withdrawal.
 *
 * <p>This LiteFlow component implements Anti-Money Laundering (AML) turnover requirement
 * validation. Players must achieve minimum turnover (typically 1x total deposits) before withdrawal
 * approval.
 *
 * <p><b>Risk Scoring Logic:</b>
 *
 * <ul>
 *   <li>Turnover Ratio >= 100%: Score 0 (Low risk, requirement met)
 *   <li>Turnover Ratio >= 50%: Score 30 (Medium risk, partial turnover)
 *   <li>Turnover Ratio < 50%: Score 100 (Critical risk, insufficient turnover, block withdrawal)
 * </ul>
 *
 * <p><b>Calculation Formula:</b>
 *
 * <pre>
 * turnoverRatio = (cumulativeTurnover / totalDeposits) * 100%
 * effectiveRatio = min(turnoverRatio, 100%) // Cap at 100%
 * </pre>
 *
 * <p><b>Business Rules:</b>
 *
 * <ul>
 *   <li>If totalDeposits = 0, turnoverRatio defaults to 100% (no deposits, no requirement)
 *   <li>If turnoverRatio < 50%, block withdrawal (set {@code blocked=true})
 *   <li>Component ID: "amlTurnoverCheck"
 * </ul>
 *
 * <p><b>Context Dependencies:</b>
 *
 * <ul>
 *   <li>Input: {@code cumulativeTurnover}, {@code totalDeposits} (pre-populated by caller)
 *   <li>Output: {@code ruleScores["amlTurnoverCheck"]}, {@code blocked}, {@code blockReason}
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@LiteflowComponent("amlTurnoverCheck")
@RequiredArgsConstructor
public class AmlTurnoverCheckCmp extends NodeComponent {

  private static final String COMPONENT_ID = "amlTurnoverCheck";

  /** Minimum turnover ratio to meet AML requirement (100% = 1x deposits). */
  private static final BigDecimal MIN_TURNOVER_RATIO = new BigDecimal("100.00");

  /** Medium risk threshold (50% turnover). */
  private static final BigDecimal MEDIUM_RISK_THRESHOLD = new BigDecimal("50.00");

  /**
   * Process AML turnover check.
   *
   * <p>This method validates player's cumulative turnover against total deposits. If turnover is
   * insufficient, the withdrawal is blocked.
   *
   * <p><b>Steps:</b>
   *
   * <ol>
   *   <li>Read cumulativeTurnover and totalDeposits from context
   *   <li>Calculate turnoverRatio (percentage)
   *   <li>Calculate risk score based on ratio
   *   <li>If score = 100 (insufficient turnover), set blocked=true
   * </ol>
   *
   * @throws Exception if processing fails
   */
  @Override
  public void process() throws Exception {
    WithdrawalRiskContext ctx = this.getContextBean(WithdrawalRiskContext.class);

    Long playerId = ctx.getPlayerId();
    BigDecimal cumulativeTurnover =
        ctx.getCumulativeTurnover() != null ? ctx.getCumulativeTurnover() : BigDecimal.ZERO;
    BigDecimal totalDeposits =
        ctx.getTotalDeposits() != null ? ctx.getTotalDeposits() : BigDecimal.ZERO;

    log.debug(
        "[AML_TURNOVER_CHECK] Processing player={}, cumulativeTurnover={}, totalDeposits={}",
        playerId,
        cumulativeTurnover,
        totalDeposits);

    // Step 1: Calculate turnover ratio
    BigDecimal turnoverRatio = calculateTurnoverRatio(cumulativeTurnover, totalDeposits);

    log.debug("[AML_TURNOVER_CHECK] Player={}, turnoverRatio={}%", playerId, turnoverRatio);

    // Step 2: Calculate risk score
    int score = calculateScore(turnoverRatio);

    // Step 3: Add score to context
    ctx.addRuleScore(COMPONENT_ID, score);

    log.debug("[AML_TURNOVER_CHECK] Player={}, score={}", playerId, score);

    // Step 4: Block withdrawal if insufficient turnover (score = 100)
    if (score >= 100) {
      ctx.setBlocked(true);
      ctx.setBlockReason(
          String.format(
              "AML turnover requirement not met. Required: %.2f%%, Actual: %.2f%%",
              MIN_TURNOVER_RATIO, turnoverRatio));

      log.warn(
          "[AML_TURNOVER_CHECK] Player={} withdrawal BLOCKED due to insufficient turnover."
              + " Required: {}%, Actual: {}%",
          playerId, MIN_TURNOVER_RATIO, turnoverRatio);
    }
  }

  /**
   * Calculate turnover ratio as percentage.
   *
   * <p>Formula: (cumulativeTurnover / totalDeposits) * 100%
   *
   * <p>If totalDeposits = 0, default to 100% (no deposits, no requirement).
   *
   * @param cumulativeTurnover player's lifetime turnover
   * @param totalDeposits player's lifetime deposits
   * @return turnover ratio as percentage (0-100+)
   */
  private BigDecimal calculateTurnoverRatio(
      BigDecimal cumulativeTurnover, BigDecimal totalDeposits) {
    // If no deposits, no turnover requirement (default 100%)
    if (totalDeposits.compareTo(BigDecimal.ZERO) == 0) {
      return new BigDecimal("100.00");
    }

    // Calculate ratio: (turnover / deposits) * 100
    BigDecimal ratio =
        cumulativeTurnover
            .divide(totalDeposits, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));

    return ratio.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Calculate risk score based on turnover ratio.
   *
   * <p><b>Scoring Logic:</b>
   *
   * <ul>
   *   <li>Ratio >= 100%: Score 0 (Low risk, requirement met)
   *   <li>Ratio >= 50%: Score 30 (Medium risk, partial turnover)
   *   <li>Ratio < 50%: Score 100 (Critical risk, insufficient turnover)
   * </ul>
   *
   * @param turnoverRatio turnover ratio as percentage
   * @return risk score (0-100)
   */
  private int calculateScore(BigDecimal turnoverRatio) {
    if (turnoverRatio.compareTo(MIN_TURNOVER_RATIO) >= 0) {
      return 0; // Low risk: Turnover >= 1x deposits (100%)
    } else if (turnoverRatio.compareTo(MEDIUM_RISK_THRESHOLD) >= 0) {
      return 30; // Medium risk: Turnover between 50%-100%
    } else {
      return 100; // Critical risk: Turnover < 50%, block withdrawal
    }
  }
}
