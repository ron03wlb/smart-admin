package net.lab1024.sa.igaming.game.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import org.springframework.stereotype.Service;

/**
 * Game lobby service — search and popular games.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class GameLobbyService {

  private static final int POPULAR_GAMES_LIMIT = 20;

  private final GameDao gameDao;
  private final GameCacheManager gameCacheManager;

  public ResponseDTO<PageResult<GameVO>> searchGames(GameQueryForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<GameVO> list = gameDao.queryPage(page, form);
    PageResult<GameVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<List<GameVO>> getPopularGames() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    List<GameVO> allGames = gameCacheManager.getGameListByTenant(tenantId);
    List<GameVO> popular =
        allGames.stream()
            .sorted(Comparator.comparingLong(GameVO::getPlayCount).reversed())
            .limit(POPULAR_GAMES_LIMIT)
            .toList();
    return ResponseDTO.ok(popular);
  }
}
