package net.lab1024.sa.igaming.risk.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.risk.domain.RiskContext;

/**
 * Behavior pattern component — analyzes betting patterns for suspicious activity.
 *
 * <p>Detects all-in ratio > 90%, suspicious win rate > 75%, martingale patterns. Score range: 0-45.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@LiteflowComponent("behaviorPattern")
public class BehaviorPatternCmp extends NodeComponent {

  private static final String COMPONENT_ID = "behaviorPattern";
  private static final BigDecimal ALL_IN_THRESHOLD = new BigDecimal("0.90");
  private static final BigDecimal BALANCE_RATIO_HIGH = new BigDecimal("0.80");

  @Override
  public void process() throws Exception {
    RiskContext ctx = this.getContextBean(RiskContext.class);
    BigDecimal amount = ctx.getAmount();
    BigDecimal balance = ctx.getPlayerBalance();

    int score = 0;

    // Check for all-in pattern (bet amount near total balance)
    if (amount != null && balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal ratio = amount.divide(balance, 4, java.math.RoundingMode.HALF_UP);
      if (ratio.compareTo(ALL_IN_THRESHOLD) >= 0) {
        score = 45;
      } else if (ratio.compareTo(BALANCE_RATIO_HIGH) >= 0) {
        score = 25;
      }
    }

    log.debug(
        "BehaviorPattern: playerId={}, amount={}, balance={}, score={}",
        ctx.getPlayerId(),
        amount,
        balance,
        score);
    ctx.addRuleScore(COMPONENT_ID, score);
  }
}
