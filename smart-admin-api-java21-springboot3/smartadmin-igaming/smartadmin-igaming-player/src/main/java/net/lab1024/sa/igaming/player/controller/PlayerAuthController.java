package net.lab1024.sa.igaming.player.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.annotation.NoNeedLogin;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.player.domain.form.PlayerLoginForm;
import net.lab1024.sa.igaming.player.domain.form.PlayerRegisterForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.player.service.PlayerAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Player Authentication Controller — registration, login, logout endpoints.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.PLAYER)
@RequiredArgsConstructor
public class PlayerAuthController {

  private final PlayerAuthService playerAuthService;

  @NoNeedLogin
  @Operation(summary = "Register a new player")
  @PostMapping("/igaming/player/auth/register")
  public ResponseDTO<PlayerAuthVO> register(@RequestBody @Valid PlayerRegisterForm form) {
    return playerAuthService.register(form);
  }

  @NoNeedLogin
  @Operation(summary = "Player login")
  @PostMapping("/igaming/player/auth/login")
  public ResponseDTO<PlayerAuthVO> login(@RequestBody @Valid PlayerLoginForm form) {
    return playerAuthService.login(form);
  }

  @NoNeedLogin
  @Operation(summary = "Player logout")
  @PostMapping("/igaming/player/auth/logout")
  public ResponseDTO<Void> logout() {
    return playerAuthService.logout();
  }
}
