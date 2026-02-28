package net.lab1024.sa.igaming.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.game.domain.form.GameAddForm;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.service.GameLobbyService;
import net.lab1024.sa.igaming.game.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Game Lobby Controller — REST API endpoints for game search and catalog.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.GAME)
@RequiredArgsConstructor
public class GameLobbyController {

  private final GameLobbyService gameLobbyService;
  private final GameService gameService;

  @Operation(summary = "Search games with pagination")
  @PostMapping("/igaming/game/lobby/search")
  @SaCheckPermission("game:lobby:query")
  public ResponseDTO<PageResult<GameVO>> searchGames(@RequestBody @Valid GameQueryForm form) {
    return gameLobbyService.searchGames(form);
  }

  @Operation(summary = "Get popular games by tenant")
  @GetMapping("/igaming/game/lobby/popular")
  @SaCheckPermission("game:lobby:query")
  public ResponseDTO<List<GameVO>> getPopularGames(@RequestParam Long tenantId) {
    return gameLobbyService.getPopularGames(tenantId);
  }

  @Operation(summary = "Get game by ID")
  @GetMapping("/igaming/game/get/{gameId}")
  @SaCheckPermission("game:lobby:query")
  public ResponseDTO<GameVO> getGame(@PathVariable Long gameId) {
    return gameService
        .getGame(gameId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(GameErrorCode.GAME_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Add a new game to catalog")
  @PostMapping("/igaming/game/add")
  @SaCheckPermission("game:lobby:operate")
  public ResponseDTO<GameVO> addGame(@RequestBody @Valid GameAddForm form) {
    return gameService.addGame(form);
  }

  @Operation(summary = "Toggle game enabled/disabled")
  @PostMapping("/igaming/game/toggle/{gameId}")
  @SaCheckPermission("game:lobby:operate")
  public ResponseDTO<Void> toggleGame(@PathVariable Long gameId, @RequestParam boolean enabled) {
    return gameService.toggleGameEnabled(gameId, enabled);
  }
}
