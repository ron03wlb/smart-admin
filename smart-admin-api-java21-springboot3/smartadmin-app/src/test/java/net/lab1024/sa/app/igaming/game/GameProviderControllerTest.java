package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.game.controller.GameProviderController;
import net.lab1024.sa.igaming.game.domain.vo.GameProviderVO;
import net.lab1024.sa.igaming.game.service.GameProviderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameProviderController unit tests — Option handling for provider lookup.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameProviderController 單元測試")
class GameProviderControllerTest {

  @Mock private GameProviderService gameProviderService;

  @InjectMocks private GameProviderController gameProviderController;

  @Test
  @DisplayName("getProvider 存在 → ok(GameProviderVO)")
  void getProvider_found() {
    GameProviderVO vo = new GameProviderVO();
    when(gameProviderService.getProvider(1L)).thenReturn(Option.some(vo));

    ResponseDTO<GameProviderVO> result = gameProviderController.getProvider(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(vo);
  }

  @Test
  @DisplayName("getProvider 不存在 → PROVIDER_NOT_FOUND")
  void getProvider_notFound() {
    when(gameProviderService.getProvider(999L)).thenReturn(Option.none());

    ResponseDTO<GameProviderVO> result = gameProviderController.getProvider(999L);

    assertThat(result.getOk()).isFalse();
    assertThat(result.getMsg()).isEqualTo(GameErrorCode.PROVIDER_NOT_FOUND.getMsg());
  }
}
