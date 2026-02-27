package net.lab1024.sa.igaming.game.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.HealthStatusEnum;
import net.lab1024.sa.igaming.game.adapter.GPAdapterFactory;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import org.springframework.stereotype.Service;

/**
 * GP health check service — pings all enabled providers.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpHealthCheckService {

  private static final Long HEALTH_CHECK_PLAYER_ID = 0L;
  private static final Long HEALTH_CHECK_TENANT_ID = 0L;

  private final GameProviderDao gameProviderDao;
  private final GPAdapterFactory gpAdapterFactory;

  /**
   * Check health of all enabled game providers.
   *
   * @return number of providers checked
   */
  public int checkAllProviders() {
    List<GameProviderEntity> providers = gameProviderDao.selectEnabledProviders();
    int checked = 0;

    for (GameProviderEntity provider : providers) {
      checkProvider(provider);
      checked++;
    }

    log.info("Health check completed for {} providers", checked);
    return checked;
  }

  private void checkProvider(GameProviderEntity provider) {
    gpAdapterFactory
        .getAdapter(provider.getProviderCode())
        .peek(
            adapter ->
                adapter
                    .getBalance(HEALTH_CHECK_PLAYER_ID, HEALTH_CHECK_TENANT_ID)
                    .onSuccess(
                        balance -> {
                          provider.setHealthStatus(HealthStatusEnum.HEALTHY.getValue());
                          provider.setLastHealthCheck(OffsetDateTime.now(ZoneId.systemDefault()));
                          gameProviderDao.updateById(provider);
                          log.debug("Provider {} is HEALTHY", provider.getProviderCode());
                        })
                    .onFailure(
                        e -> {
                          provider.setHealthStatus(HealthStatusEnum.DOWN.getValue());
                          provider.setLastHealthCheck(OffsetDateTime.now(ZoneId.systemDefault()));
                          gameProviderDao.updateById(provider);
                          log.warn(
                              "Provider {} is DOWN: {}",
                              provider.getProviderCode(),
                              e.getMessage());
                        }));
  }
}
