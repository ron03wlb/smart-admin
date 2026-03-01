package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.game.controller.GameCacheAdminController;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.service.GameCacheAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameCacheAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameCacheAdminController 單元測試")
class GameCacheAdminControllerTest {

  @Mock private GameCacheAdminService gameCacheAdminService;
  @InjectMocks private GameCacheAdminController controller;

  @Test
  @DisplayName("getGameList — 委託 Service 成功")
  void getGameList_success() {
    GameVO vo = new GameVO();
    vo.setGameId(1L);
    when(gameCacheAdminService.getGameList()).thenReturn(ResponseDTO.ok(List.of(vo)));

    ResponseDTO<List<GameVO>> result = controller.getGameList();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }

  @Test
  @DisplayName("getGamesByCategory — 委託 Service 成功")
  void getGamesByCategory_success() {
    GameVO vo = new GameVO();
    vo.setGameId(2L);
    when(gameCacheAdminService.getGamesByCategory(1)).thenReturn(ResponseDTO.ok(List.of(vo)));

    ResponseDTO<List<GameVO>> result = controller.getGamesByCategory(1);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }

  @Test
  @DisplayName("evictCache — 委託 Service 成功")
  void evictCache_success() {
    when(gameCacheAdminService.evictCache()).thenReturn(ResponseDTO.ok());

    ResponseDTO<String> result = controller.evictCache();

    assertThat(result.getOk()).isTrue();
  }
}
