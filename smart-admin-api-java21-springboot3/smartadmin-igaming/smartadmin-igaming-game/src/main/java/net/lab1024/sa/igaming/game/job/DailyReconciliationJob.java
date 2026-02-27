package net.lab1024.sa.igaming.game.job;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * Daily reconciliation job — Layer 3 reconciliation (daily at 02:00).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReconciliationJob implements SmartJob {

  private final ReconciliationManager reconciliationManager;

  @Override
  public String run(String param) {
    LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
    reconciliationManager.dailyBatchReconciliation(yesterday);
    String result = "Daily reconciliation completed for " + yesterday;
    log.info(result);
    return result;
  }
}
