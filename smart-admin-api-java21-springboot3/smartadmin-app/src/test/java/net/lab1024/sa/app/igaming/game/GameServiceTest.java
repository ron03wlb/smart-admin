package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.form.GameAddForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import net.lab1024.sa.igaming.game.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameService unit tests — Game CRUD operations.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService 單元測試")
class GameServiceTest {

  @Mock private GameDao gameDao;
  @Mock private GameCacheManager gameCacheManager;

  @InjectMocks private GameService gameService;

  @Nested
  @DisplayName("getGame")
  class GetGameTests {

    @Test
    @DisplayName("遊戲存在 → 返回 Option.some(GameVO)")
    void getGame_found() {
      GameEntity entity = buildGameEntity(1L, false);
      when(gameDao.selectById(1L)).thenReturn(entity);

      Option<GameVO> result = gameService.getGame(1L);

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getGameId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("遊戲不存在 → 返回 Option.none()")
    void getGame_notFound() {
      when(gameDao.selectById(99L)).thenReturn(null);

      Option<GameVO> result = gameService.getGame(99L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("遊戲已刪除 → 返回 Option.none()")
    void getGame_deleted() {
      GameEntity entity = buildGameEntity(2L, true);
      when(gameDao.selectById(2L)).thenReturn(entity);

      Option<GameVO> result = gameService.getGame(2L);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("addGame")
  class AddGameTests {

    @Test
    @DisplayName("新增成功 → 返回 GameVO + 快取失效")
    void addGame_success() {
      GameAddForm form = new GameAddForm();
      form.setProviderId(10L);
      form.setGameCode("SLOT_001");
      form.setGameName("Lucky Spin");
      form.setCategory(1);

      ResponseDTO<GameVO> result = gameService.addGame(form);

      assertThat(result.getOk()).isTrue();
      verify(gameDao).insert(any(GameEntity.class));
      verify(gameCacheManager).evictGameCache(any());
    }
  }

  @Nested
  @DisplayName("toggleGameEnabled")
  class ToggleGameEnabledTests {

    @Test
    @DisplayName("啟用/停用成功 → 更新 + 快取失效")
    void toggleGameEnabled_success() {
      GameEntity entity = buildGameEntity(3L, false);
      when(gameDao.selectById(3L)).thenReturn(entity);

      ResponseDTO<Void> result = gameService.toggleGameEnabled(3L, false);

      assertThat(result.getOk()).isTrue();
      assertThat(entity.getEnabled()).isFalse();
      verify(gameDao).updateById(entity);
      verify(gameCacheManager).evictGameCache(entity.getTenantId());
    }

    @Test
    @DisplayName("遊戲不存在 → 返回錯誤")
    void toggleGameEnabled_notFound() {
      when(gameDao.selectById(99L)).thenReturn(null);

      ResponseDTO<Void> result = gameService.toggleGameEnabled(99L, true);

      assertThat(result.getOk()).isFalse();
      verify(gameDao, never()).updateById(any(GameEntity.class));
      verify(gameCacheManager, never()).evictGameCache(any());
    }
  }

  // --- Helpers ---

  private GameEntity buildGameEntity(Long gameId, boolean deleted) {
    GameEntity entity = new GameEntity();
    entity.setGameId(gameId);
    entity.setProviderId(10L);
    entity.setGameCode("GAME_" + gameId);
    entity.setGameName("Test Game " + gameId);
    entity.setCategory(1);
    entity.setPlayCount(0L);
    entity.setEnabled(true);
    entity.setDeleted(deleted);
    entity.setTenantId(1L);
    return entity;
  }
}
