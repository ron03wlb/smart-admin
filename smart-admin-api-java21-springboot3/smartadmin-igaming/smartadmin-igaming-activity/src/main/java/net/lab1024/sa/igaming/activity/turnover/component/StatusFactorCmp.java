package net.lab1024.sa.igaming.activity.turnover.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverStatusFactorRuleManager;

/**
 * Layer 2 Settlement Status Factor Component — applies status-specific multiplier to effective
 * turnover.
 *
 * <p>Status factor logic:
 *
 * <ul>
 *   <li><b>WIN/HALF_WIN/HALF_LOSS</b> (status 1,7,8): 100% - full turnover counts
 *   <li><b>LOSS</b> (status 2): 0% - no turnover (player-friendly wagering rule)
 *   <li><b>DRAW/TIE/VOID/CANCEL/RUNNING</b> (status 3,4,5,6,9): 0% - no turnover (bet cancelled or
 *       unsettled)
 * </ul>
 *
 * <p>Calculation:
 *
 * <pre>
 * validTurnoverFinance = effectiveTurnoverBase * statusFactor / 100
 * </pre>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>statusFactor: BigDecimal (0.00 - 100.00)
 *   <li>validTurnoverFinance: BigDecimal (final amount for financial reporting)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Slf4j
@LiteflowComponent("statusFactorNode")
@RequiredArgsConstructor
public class StatusFactorCmp extends NodeComponent {

  private final TurnoverStatusFactorRuleManager statusFactorRuleManager;

  @Override
  public void process() throws Exception {
    TurnoverContext ctx = this.getContextBean(TurnoverContext.class);

    // Skip if already rejected by Layer 1
    if (ctx.isRejected()) {
      ctx.setStatusFactor(BigDecimal.ZERO);
      ctx.setValidTurnoverFinance(BigDecimal.ZERO);
      return;
    }

    // Query status factor rule
    BigDecimal statusFactor = getStatusFactor(ctx);

    // Calculate valid turnover for finance
    BigDecimal validTurnover = calculateValidTurnover(ctx, statusFactor);

    ctx.setStatusFactor(statusFactor);
    ctx.setValidTurnoverFinance(validTurnover);

    log.debug(
        "StatusFactor complete: betId={}, settlementStatus={}, statusFactor={}, validTurnover={}",
        ctx.getBetId(),
        ctx.getSettlementStatus(),
        statusFactor,
        validTurnover);
  }

  /**
   * Get status factor percentage for settlement status.
   *
   * @param ctx turnover context
   * @return status factor percentage (0.00 - 100.00)
   */
  private BigDecimal getStatusFactor(TurnoverContext ctx) {
    Long tenantId = ctx.getTenantId();
    Integer settlementStatus = ctx.getSettlementStatus();

    if (settlementStatus == null) {
      log.warn("Missing settlement status: betId={}", ctx.getBetId());
      return BigDecimal.ZERO;
    }

    // Query active status factor rule from Manager (uses cache)
    BigDecimal factor = statusFactorRuleManager.getStatusFactor(tenantId, settlementStatus);

    if (factor == null) {
      log.warn(
          "No status factor rule found: tenantId={}, settlementStatus={}",
          tenantId,
          settlementStatus);
      // Default: 0% for unknown status (conservative approach)
      return BigDecimal.ZERO;
    }

    return factor;
  }

  /**
   * Calculate valid turnover for finance = effectiveTurnoverBase * statusFactor / 100.
   *
   * @param ctx turnover context
   * @param statusFactor status factor percentage
   * @return valid turnover for finance
   */
  private BigDecimal calculateValidTurnover(TurnoverContext ctx, BigDecimal statusFactor) {
    BigDecimal effectiveTurnoverBase = ctx.getEffectiveTurnoverBase();

    if (effectiveTurnoverBase == null) {
      log.warn("Missing effective turnover base: betId={}", ctx.getBetId());
      return BigDecimal.ZERO;
    }

    // validTurnoverFinance = effectiveTurnoverBase * statusFactor / 100
    return effectiveTurnoverBase
        .multiply(statusFactor)
        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
  }
}
