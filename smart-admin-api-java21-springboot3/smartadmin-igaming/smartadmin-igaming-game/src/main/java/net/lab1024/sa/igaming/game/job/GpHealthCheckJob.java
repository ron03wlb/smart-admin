package net.lab1024.sa.igaming.game.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.game.service.GpHealthCheckService;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * GP health check job — periodic provider health monitoring.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GpHealthCheckJob implements SmartJob {

  private final GpHealthCheckService gpHealthCheckService;

  @Override
  public String run(String param) {
    int count = gpHealthCheckService.checkAllProviders();
    String result = "Health check completed for " + count + " providers";
    log.info(result);
    return result;
  }
}
