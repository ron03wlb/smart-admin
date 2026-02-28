package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameCacheManager unit tests — game list caching delegation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameCacheManager 單元測試")
class GameCacheManagerTest {

  @Mock private GameDao gameDao;

  @InjectMocks private GameCacheManager gameCacheManager;

  @Test
  @DisplayName("getGameListByTenant → 委派至 gameDao.selectEnabledGamesByTenant")
  void getGameListByTenant_delegatesToDao() {
    GameVO vo = new GameVO();
    vo.setGameId(1L);
    when(gameDao.selectEnabledGamesByTenant(1L)).thenReturn(List.of(vo));

    List<GameVO> result = gameCacheManager.getGameListByTenant(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getGameId()).isEqualTo(1L);
    verify(gameDao).selectEnabledGamesByTenant(1L);
  }

  @Test
  @DisplayName("getGamesByCategory → 委派至 gameDao.selectGamesByTenantAndCategory")
  void getGamesByCategory_delegatesToDao() {
    GameVO vo = new GameVO();
    vo.setGameId(2L);
    when(gameDao.selectGamesByTenantAndCategory(1L, 3)).thenReturn(List.of(vo));

    List<GameVO> result = gameCacheManager.getGamesByCategory(1L, 3);

    assertThat(result).hasSize(1);
    verify(gameDao).selectGamesByTenantAndCategory(1L, 3);
  }

  @Test
  @DisplayName("evictGameCache → 無業務邏輯，不拋異常")
  void evictGameCache_noBusinessLogic() {
    gameCacheManager.evictGameCache(1L);
    // No business logic — cache eviction handled by Spring @CacheEvict
  }
}
