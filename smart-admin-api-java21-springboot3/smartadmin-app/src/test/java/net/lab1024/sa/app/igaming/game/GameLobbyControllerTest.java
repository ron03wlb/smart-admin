package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.game.controller.GameLobbyController;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.service.GameLobbyService;
import net.lab1024.sa.igaming.game.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameLobbyController unit tests — Option handling for game lookup.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameLobbyController 單元測試")
class GameLobbyControllerTest {

  @Mock private GameLobbyService gameLobbyService;
  @Mock private GameService gameService;

  @InjectMocks private GameLobbyController gameLobbyController;

  @Test
  @DisplayName("getGame 存在 → ok(GameVO)")
  void getGame_found() {
    GameVO vo = new GameVO();
    when(gameService.getGame(1L)).thenReturn(Option.some(vo));

    ResponseDTO<GameVO> result = gameLobbyController.getGame(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(vo);
  }

  @Test
  @DisplayName("getGame 不存在 → GAME_NOT_FOUND")
  void getGame_notFound() {
    when(gameService.getGame(999L)).thenReturn(Option.none());

    ResponseDTO<GameVO> result = gameLobbyController.getGame(999L);

    assertThat(result.getOk()).isFalse();
    assertThat(result.getMsg()).isEqualTo(GameErrorCode.GAME_NOT_FOUND.getMsg());
  }
}
