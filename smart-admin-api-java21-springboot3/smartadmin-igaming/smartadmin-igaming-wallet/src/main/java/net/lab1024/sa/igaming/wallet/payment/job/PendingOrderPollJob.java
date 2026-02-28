package net.lab1024.sa.igaming.wallet.payment.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.wallet.payment.service.ReconciliationService;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * Pending order poll job — Layer 2 reconciliation (poll PSP for stale orders).
 *
 * <p>Suggested schedule: every 5-10 minutes.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderPollJob implements SmartJob {

  private static final int PENDING_MINUTES_THRESHOLD = 30;

  private final ReconciliationService reconciliationService;

  @Override
  public String run(String param) {
    int processed = reconciliationService.reconcilePendingOrders(PENDING_MINUTES_THRESHOLD);
    String result = "Polled pending orders: " + processed + " processed";
    log.info(result);
    return result;
  }
}
