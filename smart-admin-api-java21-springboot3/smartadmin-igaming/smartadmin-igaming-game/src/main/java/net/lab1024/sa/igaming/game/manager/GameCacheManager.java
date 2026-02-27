package net.lab1024.sa.igaming.game.manager;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Game cache manager — handles game list caching.
 *
 * <p>{@code @Cacheable} / {@code @CacheEvict} must reside in Manager layer per SmartAdmin rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Component
@RequiredArgsConstructor
public class GameCacheManager {

  private final GameDao gameDao;

  @Cacheable(value = "game:list", key = "'tenant:' + #tenantId")
  public List<GameVO> getGameListByTenant(Long tenantId) {
    return gameDao.selectEnabledGamesByTenant(tenantId);
  }

  @Cacheable(value = "game:category", key = "'tenant:' + #tenantId + ':cat:' + #category")
  public List<GameVO> getGamesByCategory(Long tenantId, Integer category) {
    return gameDao.selectGamesByTenantAndCategory(tenantId, category);
  }

  @CacheEvict(
      value = {"game:list", "game:category"},
      allEntries = true)
  public void evictGameCache(Long tenantId) {
    // Cache eviction only — no business logic
  }
}
