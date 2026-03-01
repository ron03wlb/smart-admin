package net.lab1024.sa.igaming.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.service.GameCacheAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Game cache admin controller — admin endpoints for game cache management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.GAME)
@RequiredArgsConstructor
public class GameCacheAdminController {

  private final GameCacheAdminService gameCacheAdminService;

  @Operation(summary = "Get cached game list (admin)")
  @GetMapping("/igaming/admin/game/cache/list")
  @SaCheckPermission("game:cache:admin")
  public ResponseDTO<List<GameVO>> getGameList() {
    return gameCacheAdminService.getGameList();
  }

  @Operation(summary = "Get cached games by category (admin)")
  @GetMapping("/igaming/admin/game/cache/category/{category}")
  @SaCheckPermission("game:cache:admin")
  public ResponseDTO<List<GameVO>> getGamesByCategory(@PathVariable Integer category) {
    return gameCacheAdminService.getGamesByCategory(category);
  }

  @Operation(summary = "Evict game cache (admin)")
  @PostMapping("/igaming/admin/game/cache/evict")
  @SaCheckPermission("game:cache:admin")
  public ResponseDTO<String> evictCache() {
    return gameCacheAdminService.evictCache();
  }
}
