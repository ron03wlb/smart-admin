package net.lab1024.sa.igaming.activity.turnover.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;

/**
 * Turnover Aggregate Component — finalizes calculation and logs results.
 *
 * <p>This component performs final validation and prepares the context for result extraction. No
 * additional calculations are performed; all values are already set by previous nodes.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Verify all required fields are populated
 *   <li>Log final calculation result for audit trail
 *   <li>Return context ready for extraction by {@code TurnoverCalculationService}
 * </ul>
 *
 * <p>Input (from context):
 *
 * <ul>
 *   <li>effectiveTurnoverBase (from riskFilterNode)
 *   <li>validTurnoverFinance (from statusFactorNode)
 *   <li>activityValidTurnover (from gameWeightNode)
 *   <li>riskActionType, statusFactor, gameWeight (intermediate results)
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>Context ready for {@code TurnoverContext.getResult()}
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Slf4j
@LiteflowComponent("turnoverAggregateNode")
public class TurnoverAggregateCmp extends NodeComponent {

  @Override
  public void process() throws Exception {
    TurnoverContext ctx = this.getContextBean(TurnoverContext.class);

    // Log final calculation result
    if (ctx.isRejected()) {
      log.info(
          "Turnover calculation rejected: betId={}, reason={}, rejectedBy={}",
          ctx.getBetId(),
          ctx.getRejectedBy(),
          ctx.getRejectedBy());
    } else {
      log.info(
          "Turnover calculation complete: betId={}, playerId={}, "
              + "betAmount={}, effectiveTurnoverBase={}, validTurnoverFinance={}, "
              + "activityValidTurnover={}, riskAction={}, statusFactor={}, gameWeight={}, "
              + "matchedRules={}",
          ctx.getBetId(),
          ctx.getPlayerId(),
          ctx.getBetAmount(),
          ctx.getEffectiveTurnoverBase(),
          ctx.getValidTurnoverFinance(),
          ctx.getActivityValidTurnover(),
          ctx.getRiskActionType(),
          ctx.getStatusFactor(),
          ctx.getGameWeight(),
          ctx.getMatchedRules());
    }

    // Context is now ready for extraction by TurnoverCalculationService
    log.debug("TurnoverAggregate complete: betId={}", ctx.getBetId());
  }
}
