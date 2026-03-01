package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import net.lab1024.sa.igaming.game.service.GameLobbyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameLobbyService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameLobbyService 單元測試")
class GameLobbyServiceTest {

  @Mock private GameDao gameDao;
  @Mock private GameCacheManager gameCacheManager;
  @InjectMocks private GameLobbyService gameLobbyService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("searchGames 分頁搜索 — 返回成功")
  void searchGames_success() {
    GameQueryForm form = new GameQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);

    GameVO vo = new GameVO();
    vo.setGameCode("slot-001");
    when(gameDao.queryPage(any(Page.class), any())).thenReturn(List.of(vo));

    ResponseDTO<PageResult<GameVO>> result = gameLobbyService.searchGames(form);
    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("getPopularGames — 返回前 20 名熱門遊戲")
  void getPopularGames_returnsTop20() {
    // Build 25 games with different play counts
    List<GameVO> games = new java.util.ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      GameVO vo = new GameVO();
      vo.setGameCode("game-" + i);
      vo.setPlayCount((long) i * 100);
      games.add(vo);
    }
    when(gameCacheManager.getGameListByTenant(1L)).thenReturn(games);

    ResponseDTO<List<GameVO>> result = gameLobbyService.getPopularGames();
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(20);
    // Verify sorted by playCount descending
    assertThat(result.getData().get(0).getPlayCount()).isEqualTo(2500L);
  }

  @Test
  @DisplayName("getPopularGames 遊戲不足 20 — 全部返回")
  void getPopularGames_lessThan20() {
    GameVO vo = new GameVO();
    vo.setGameCode("game-1");
    vo.setPlayCount(100L);
    when(gameCacheManager.getGameListByTenant(1L)).thenReturn(List.of(vo));

    ResponseDTO<List<GameVO>> result = gameLobbyService.getPopularGames();
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }
}
