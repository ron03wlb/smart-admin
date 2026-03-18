package net.lab1024.sa.igaming.activity.turnover.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import io.vavr.control.Option;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverOddsThresholdRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverOddsThresholdRuleManager;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverRiskActionRuleManager;

/**
 * Layer 1 Risk Filter Component — validates odds threshold and applies risk-based turnover
 * adjustments.
 *
 * <p>Validation logic:
 *
 * <ul>
 *   <li><b>Odds Threshold Check</b>: Rejects bets with odds below minimum threshold (e.g., 1.5)
 *   <li><b>Risk Action Determination</b>: Maps risk score (0-100) to action types:
 *       <ul>
 *         <li>0-29: RA_PASS (factor=100%)
 *         <li>30-69: RA_FLAG (factor=100%, create proposal for review)
 *         <li>70+: RA_BLOCK (factor=0%, reject turnover)
 *       </ul>
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>riskActionType: Integer (1=PASS, 2=FLAG, 3=BLOCK, 4=MANUAL_REVIEW)
 *   <li>turnoverFactor: BigDecimal (0.00 - 100.00)
 *   <li>rejected: Boolean (true if odds < threshold OR risk action = BLOCK)
 *   <li>effectiveTurnoverBase: BigDecimal = betAmount * turnoverFactor / 100
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Slf4j
@LiteflowComponent("riskFilterNode")
@RequiredArgsConstructor
public class RiskFilterCmp extends NodeComponent {

  // Risk level thresholds
  private static final int RISK_SCORE_CRITICAL_THRESHOLD = 70;
  private static final int RISK_SCORE_HIGH_THRESHOLD = 50;
  private static final int RISK_SCORE_MEDIUM_THRESHOLD = 30;

  // Risk action types
  private static final int RISK_ACTION_BLOCK = 3;

  // Default comparison operator
  private static final String DEFAULT_COMPARISON_OPERATOR = ">=";

  private final TurnoverOddsThresholdRuleManager oddsThresholdRuleManager;
  private final TurnoverRiskActionRuleManager riskActionRuleManager;

  @Override
  public void process() throws Exception {
    TurnoverContext ctx = this.getContextBean(TurnoverContext.class);

    // Step 1: Validate odds threshold
    if (!validateOddsThreshold(ctx)) {
      log.warn(
          "Bet rejected: odds too low. betId={}, odds={}, oddsType={}",
          ctx.getBetId(),
          ctx.getOddsValue(),
          ctx.getOddsType());
      ctx.markRejected("ODDS_TOO_LOW");
      return;
    }

    // Step 2: Determine risk action based on risk score
    applyRiskAction(ctx);

    // Step 3: Calculate effective turnover base
    calculateEffectiveTurnover(ctx);

    log.debug(
        "RiskFilter complete: betId={}, riskScore={}, riskAction={}, turnoverFactor={}, effectiveTurnover={}",
        ctx.getBetId(),
        ctx.getRiskScore(),
        ctx.getRiskActionType(),
        ctx.getTurnoverFactor(),
        ctx.getEffectiveTurnoverBase());
  }

  /**
   * Validate if odds meet minimum threshold requirement.
   *
   * @param ctx turnover context
   * @return true if odds >= threshold, false otherwise
   */
  private boolean validateOddsThreshold(TurnoverContext ctx) {
    Long tenantId = ctx.getTenantId();
    Integer oddsType = ctx.getOddsType();
    BigDecimal oddsValue = ctx.getOddsValue();

    if (oddsValue == null || oddsType == null) {
      log.warn("Missing odds data: betId={}", ctx.getBetId());
      return false;
    }

    // Query active odds threshold rule from cache
    Option<TurnoverOddsThresholdRuleEntity> ruleOpt =
        oddsThresholdRuleManager.getActiveRule(tenantId, oddsType);

    if (ruleOpt.isEmpty()) {
      log.warn("No odds threshold rule found: tenantId={}, oddsType={}", tenantId, oddsType);
      return true; // Default allow if no rule configured
    }

    TurnoverOddsThresholdRuleEntity rule = ruleOpt.get();
    ctx.addMatchedRule(rule.getRuleCode());

    // Compare odds value with threshold using comparison operator
    return compareOdds(oddsValue, rule.getThresholdValue(), rule.getComparisonOperator());
  }

  /**
   * Compare odds value against threshold using specified operator.
   *
   * @param oddsValue actual odds value
   * @param threshold threshold value
   * @param operator comparison operator (>=, >, <=, <, =)
   * @return true if comparison passes, false otherwise
   */
  private boolean compareOdds(BigDecimal oddsValue, BigDecimal threshold, String operator) {
    // Use default operator if not specified
    String effectiveOperator = (operator != null) ? operator : DEFAULT_COMPARISON_OPERATOR;

    return switch (effectiveOperator) {
      case ">=" -> oddsValue.compareTo(threshold) >= 0;
      case ">" -> oddsValue.compareTo(threshold) > 0;
      case "<=" -> oddsValue.compareTo(threshold) <= 0;
      case "<" -> oddsValue.compareTo(threshold) < 0;
      case "=" -> oddsValue.compareTo(threshold) == 0;
      default -> {
        log.warn("Unknown comparison operator: {}, defaulting to >=", effectiveOperator);
        yield oddsValue.compareTo(threshold) >= 0;
      }
    };
  }

  /**
   * Apply risk action based on risk score level.
   *
   * @param ctx turnover context
   */
  private void applyRiskAction(TurnoverContext ctx) {
    Long tenantId = ctx.getTenantId();
    Integer riskScore = ctx.getRiskScore() != null ? ctx.getRiskScore() : 0;

    // Determine risk level (1-4) based on score
    int riskLevel = determineRiskLevel(riskScore);

    // Query active risk action rule from cache
    Option<TurnoverRiskActionRuleEntity> ruleOpt =
        riskActionRuleManager.getActiveRule(tenantId, riskLevel);

    if (ruleOpt.isEmpty()) {
      log.warn("No risk action rule found: tenantId={}, riskLevel={}", tenantId, riskLevel);
      // Default action: PASS with 100% factor
      ctx.setRiskActionType(1); // RA_PASS
      ctx.setTurnoverFactor(new BigDecimal("100.00"));
      return;
    }

    TurnoverRiskActionRuleEntity rule = ruleOpt.get();
    ctx.addMatchedRule(rule.getRuleCode());

    // Apply rule action and factor
    ctx.setRiskActionType(rule.getActionType());
    ctx.setTurnoverFactor(rule.getTurnoverFactor());

    // Check if bet should be blocked
    if (rule.getActionType() == RISK_ACTION_BLOCK) {
      ctx.markRejected("RISK_SCORE_HIGH");
    }
  }

  /**
   * Determine risk level based on risk score.
   *
   * @param riskScore risk score (0-100)
   * @return risk level (1: LOW, 2: MEDIUM, 3: HIGH, 4: CRITICAL)
   */
  private int determineRiskLevel(int riskScore) {
    if (riskScore >= RISK_SCORE_CRITICAL_THRESHOLD) {
      return 4; // CRITICAL
    } else if (riskScore >= RISK_SCORE_HIGH_THRESHOLD) {
      return 3; // HIGH
    } else if (riskScore >= RISK_SCORE_MEDIUM_THRESHOLD) {
      return 2; // MEDIUM
    } else {
      return 1; // LOW
    }
  }

  /**
   * Calculate effective turnover base = betAmount * turnoverFactor / 100.
   *
   * @param ctx turnover context
   */
  private void calculateEffectiveTurnover(TurnoverContext ctx) {
    if (ctx.isRejected()) {
      ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);
      return;
    }

    BigDecimal betAmount = ctx.getBetAmount();
    BigDecimal turnoverFactor = ctx.getTurnoverFactor();

    if (betAmount == null || turnoverFactor == null) {
      log.warn("Missing calculation data: betId={}", ctx.getBetId());
      ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);
      return;
    }

    BigDecimal effectiveTurnover =
        betAmount
            .multiply(turnoverFactor)
            .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

    ctx.setEffectiveTurnoverBase(effectiveTurnover);
  }
}
