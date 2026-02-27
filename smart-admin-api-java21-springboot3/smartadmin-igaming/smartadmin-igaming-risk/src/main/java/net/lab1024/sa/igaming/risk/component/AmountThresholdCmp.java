package net.lab1024.sa.igaming.risk.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.risk.domain.RiskContext;

/**
 * Amount threshold component — monitors transaction amounts against configurable limits.
 *
 * <p>Flags single bets > $10k or withdrawals > $50k. Score range: 0-60.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@LiteflowComponent("amountThreshold")
public class AmountThresholdCmp extends NodeComponent {

  private static final String COMPONENT_ID = "amountThreshold";
  private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("50000");
  private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("10000");
  private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("5000");

  @Override
  public void process() throws Exception {
    RiskContext ctx = this.getContextBean(RiskContext.class);
    BigDecimal amount = ctx.getAmount();

    int score = 0;
    if (amount != null) {
      score = calculateScore(amount);
    }

    log.debug(
        "AmountThreshold: playerId={}, amount={}, score={}", ctx.getPlayerId(), amount, score);
    ctx.addRuleScore(COMPONENT_ID, score);
  }

  private int calculateScore(BigDecimal amount) {
    if (amount.compareTo(CRITICAL_THRESHOLD) >= 0) {
      return 60;
    } else if (amount.compareTo(HIGH_THRESHOLD) >= 0) {
      return 40;
    } else if (amount.compareTo(MEDIUM_THRESHOLD) >= 0) {
      return 20;
    }
    return 0;
  }
}
