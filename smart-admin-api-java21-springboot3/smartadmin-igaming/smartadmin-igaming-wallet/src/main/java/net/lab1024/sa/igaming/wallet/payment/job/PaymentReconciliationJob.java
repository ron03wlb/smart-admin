package net.lab1024.sa.igaming.wallet.payment.job;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentReconciliationManager;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * Payment daily reconciliation job — Layer 3 batch reconciliation.
 *
 * <p>Suggested schedule: daily at 02:00 (after game reconciliation).
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob implements SmartJob {

  private final PaymentReconciliationManager reconciliationManager;

  @Override
  public String run(String param) {
    LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
    reconciliationManager.dailyBatchReconciliation(yesterday);
    String result = "Payment reconciliation completed for " + yesterday;
    log.info(result);
    return result;
  }
}
