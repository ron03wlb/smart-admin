package net.lab1024.sa.igaming.activity.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.manager.BonusLifecycleManager;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * Bonus expiration job — periodically expires overdue active bonuses.
 *
 * <p>Recommended schedule: every 30 minutes.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BonusExpirationJob implements SmartJob {

  private static final int BATCH_SIZE = 100;

  private final BonusLifecycleManager bonusLifecycleManager;

  @Override
  public String run(String param) {
    int count = bonusLifecycleManager.expireActiveBonuses(BATCH_SIZE);
    String result = "Bonus expiration: expired " + count + " records";
    log.info(result);
    return result;
  }
}
