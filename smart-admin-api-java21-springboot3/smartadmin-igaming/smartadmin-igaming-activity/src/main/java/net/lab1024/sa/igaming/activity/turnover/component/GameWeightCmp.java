package net.lab1024.sa.igaming.activity.turnover.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverGameWeightRuleManager;

/**
 * Layer 3 Game Weight Component — applies game-specific weight multiplier to valid turnover.
 *
 * <p>Game weight logic (default values):
 *
 * <ul>
 *   <li><b>Slots (category 1)</b>: 100% - full turnover counts
 *   <li><b>Live Casino (category 2)</b>: 15% - low contribution (high RTP games like Baccarat)
 *   <li><b>Sports Betting (category 3)</b>: 100% - full turnover counts
 *   <li><b>Poker (category 4)</b>: 5% - very low contribution (skill-based game)
 *   <li><b>Table Games (category 5)</b>: 20% - low contribution (high RTP games like Blackjack)
 *   <li><b>Lottery (category 6)</b>: 15% - low contribution
 * </ul>
 *
 * <p>Calculation:
 *
 * <pre>
 * activityValidTurnover = validTurnoverFinance * gameWeight / 100
 * </pre>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>gameWeight: BigDecimal (0.00 - 100.00)
 *   <li>activityValidTurnover: BigDecimal (final amount for bonus wagering requirements)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Slf4j
@LiteflowComponent("gameWeightNode")
@RequiredArgsConstructor
public class GameWeightCmp extends NodeComponent {

  private final TurnoverGameWeightRuleManager gameWeightRuleManager;

  @Override
  public void process() throws Exception {
    TurnoverContext ctx = this.getContextBean(TurnoverContext.class);

    // Skip if already rejected by Layer 1
    if (ctx.isRejected()) {
      ctx.setGameWeight(BigDecimal.ZERO);
      ctx.setActivityValidTurnover(BigDecimal.ZERO);
      return;
    }

    // Query game weight rule
    BigDecimal gameWeight = getGameWeight(ctx);

    // Calculate activity valid turnover
    BigDecimal activityValidTurnover = calculateActivityValidTurnover(ctx, gameWeight);

    ctx.setGameWeight(gameWeight);
    ctx.setActivityValidTurnover(activityValidTurnover);

    log.debug(
        "GameWeight complete: betId={}, gameCategory={}, gameWeight={}, activityValidTurnover={}",
        ctx.getBetId(),
        ctx.getGameCategory(),
        gameWeight,
        activityValidTurnover);
  }

  /**
   * Get game weight percentage for game category.
   *
   * @param ctx turnover context
   * @return game weight percentage (0.00 - 100.00)
   */
  private BigDecimal getGameWeight(TurnoverContext ctx) {
    Long tenantId = ctx.getTenantId();
    Integer gameCategory = ctx.getGameCategory();

    if (gameCategory == null) {
      log.warn("Missing game category: betId={}", ctx.getBetId());
      return BigDecimal.ZERO;
    }

    // Query active game weight rule from Manager (uses cache)
    BigDecimal weight = gameWeightRuleManager.getGameWeight(tenantId, gameCategory);

    if (weight == null) {
      log.warn("No game weight rule found: tenantId={}, gameCategory={}", tenantId, gameCategory);
      // Default: 0% for unknown category (conservative approach)
      return BigDecimal.ZERO;
    }

    return weight;
  }

  /**
   * Calculate activity valid turnover = validTurnoverFinance * gameWeight / 100.
   *
   * @param ctx turnover context
   * @param gameWeight game weight percentage
   * @return activity valid turnover
   */
  private BigDecimal calculateActivityValidTurnover(TurnoverContext ctx, BigDecimal gameWeight) {
    BigDecimal validTurnoverFinance = ctx.getValidTurnoverFinance();

    if (validTurnoverFinance == null) {
      log.warn("Missing valid turnover finance: betId={}", ctx.getBetId());
      return BigDecimal.ZERO;
    }

    // activityValidTurnover = validTurnoverFinance * gameWeight / 100
    return validTurnoverFinance
        .multiply(gameWeight)
        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
  }
}
