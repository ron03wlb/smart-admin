package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.controller.PlayerAuthController;
import net.lab1024.sa.igaming.player.domain.form.PlayerLoginForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerRegisterForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.player.service.PlayerAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerAuthController unit tests — public authentication delegation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerAuthController 單元測試")
class PlayerAuthControllerTest {

  @Mock private PlayerAuthService playerAuthService;

  @InjectMocks private PlayerAuthController playerAuthController;

  @Test
  @DisplayName("register → 委派至 playerAuthService.register")
  void register_delegatesToService() {
    PlayerRegisterForm form = new PlayerRegisterForm();
    PlayerAuthVO authVO = new PlayerAuthVO();
    ResponseDTO<PlayerAuthVO> expected = ResponseDTO.ok(authVO);
    when(playerAuthService.register(form)).thenReturn(expected);

    ResponseDTO<PlayerAuthVO> result = playerAuthController.register(form);

    assertThat(result).isEqualTo(expected);
    verify(playerAuthService).register(form);
  }

  @Test
  @DisplayName("login → 委派至 playerAuthService.login")
  void login_delegatesToService() {
    PlayerLoginForm form = new PlayerLoginForm();
    PlayerAuthVO authVO = new PlayerAuthVO();
    ResponseDTO<PlayerAuthVO> expected = ResponseDTO.ok(authVO);
    when(playerAuthService.login(form)).thenReturn(expected);

    ResponseDTO<PlayerAuthVO> result = playerAuthController.login(form);

    assertThat(result).isEqualTo(expected);
    verify(playerAuthService).login(form);
  }
}
