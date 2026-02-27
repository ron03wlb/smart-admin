package net.lab1024.sa.igaming.activity.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.manager.VipAutoEvaluationManager;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * VIP evaluation job — daily evaluation of player VIP levels.
 *
 * <p>Recommended schedule: daily at 04:00.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipEvaluationJob implements SmartJob {

  private final VipAutoEvaluationManager vipAutoEvaluationManager;

  @Override
  public String run(String param) {
    int evaluated = vipAutoEvaluationManager.evaluateAllActivePlayers();
    String result = "VIP evaluation: evaluated " + evaluated + " players";
    log.info(result);
    return result;
  }
}
