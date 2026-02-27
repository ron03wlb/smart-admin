package net.lab1024.sa.igaming.game.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * Missing settlement poll job — Layer 2 reconciliation (every 5 minutes).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissingSettlementPollJob implements SmartJob {

  private final ReconciliationManager reconciliationManager;

  @Override
  public String run(String param) {
    int count = reconciliationManager.pollMissingSettlements();
    String result = "Polled missing settlements, processed " + count + " rounds";
    log.info(result);
    return result;
  }
}
