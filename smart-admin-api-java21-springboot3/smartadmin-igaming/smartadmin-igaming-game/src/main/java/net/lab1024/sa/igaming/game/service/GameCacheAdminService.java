package net.lab1024.sa.igaming.game.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import org.springframework.stereotype.Service;

/**
 * Game cache admin service — admin operations for game cache management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameCacheAdminService {

  private final GameCacheManager gameCacheManager;

  /**
   * Get cached game list for the current tenant.
   *
   * @return list of game VOs from cache
   */
  public ResponseDTO<List<GameVO>> getGameList() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    List<GameVO> games = gameCacheManager.getGameListByTenant(tenantId);
    return ResponseDTO.ok(games);
  }

  /**
   * Get cached games by category for the current tenant.
   *
   * @param category game category enum value
   * @return list of game VOs from cache
   */
  public ResponseDTO<List<GameVO>> getGamesByCategory(Integer category) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    List<GameVO> games = gameCacheManager.getGamesByCategory(tenantId, category);
    return ResponseDTO.ok(games);
  }

  /**
   * Evict game cache for the current tenant.
   *
   * @return success response
   */
  public ResponseDTO<String> evictCache() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    gameCacheManager.evictGameCache(tenantId);
    log.info("Game cache evicted by admin: tenantId={}", tenantId);
    return ResponseDTO.ok();
  }
}
