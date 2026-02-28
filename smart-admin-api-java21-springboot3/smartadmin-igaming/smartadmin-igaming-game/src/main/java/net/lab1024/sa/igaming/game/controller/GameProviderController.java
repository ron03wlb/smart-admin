package net.lab1024.sa.igaming.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.game.domain.form.GameProviderAddForm;
import net.lab1024.sa.igaming.game.domain.form.GameProviderQueryForm;
import net.lab1024.sa.igaming.game.domain.form.GameProviderUpdateForm;
import net.lab1024.sa.igaming.game.domain.vo.GameProviderVO;
import net.lab1024.sa.igaming.game.service.GameProviderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Game Provider Controller — REST API endpoints for GP configuration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.GAME)
@RequiredArgsConstructor
public class GameProviderController {

  private final GameProviderService gameProviderService;

  @Operation(summary = "Get game provider by ID")
  @GetMapping("/igaming/game/provider/get/{providerId}")
  @SaCheckPermission("game:provider:query")
  public ResponseDTO<GameProviderVO> getProvider(@PathVariable Long providerId) {
    return gameProviderService
        .getProvider(providerId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(GameErrorCode.PROVIDER_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Query game providers with pagination")
  @PostMapping("/igaming/game/provider/query")
  @SaCheckPermission("game:provider:query")
  public ResponseDTO<PageResult<GameProviderVO>> queryProviders(
      @RequestBody @Valid GameProviderQueryForm form) {
    return gameProviderService.queryProviders(form);
  }

  @Operation(summary = "Add a new game provider")
  @PostMapping("/igaming/game/provider/add")
  @SaCheckPermission("game:provider:operate")
  public ResponseDTO<GameProviderVO> addProvider(@RequestBody @Valid GameProviderAddForm form) {
    return gameProviderService.addProvider(form);
  }

  @Operation(summary = "Update game provider configuration")
  @PutMapping("/igaming/game/provider/update")
  @SaCheckPermission("game:provider:operate")
  public ResponseDTO<Void> updateProvider(@RequestBody @Valid GameProviderUpdateForm form) {
    return gameProviderService.updateProvider(form);
  }

  @Operation(summary = "Enable a game provider")
  @PostMapping("/igaming/game/provider/enable/{providerId}")
  @SaCheckPermission("game:provider:operate")
  public ResponseDTO<Void> enableProvider(@PathVariable Long providerId) {
    return gameProviderService.enableProvider(providerId);
  }

  @Operation(summary = "Disable a game provider")
  @PostMapping("/igaming/game/provider/disable/{providerId}")
  @SaCheckPermission("game:provider:operate")
  public ResponseDTO<Void> disableProvider(@PathVariable Long providerId) {
    return gameProviderService.disableProvider(providerId);
  }
}
