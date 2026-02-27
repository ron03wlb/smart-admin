package net.lab1024.sa.igaming.game.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.form.GameAddForm;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import org.springframework.stereotype.Service;

/**
 * Game service — CRUD for game catalog.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class GameService {

  private final GameDao gameDao;
  private final GameCacheManager gameCacheManager;

  public Option<GameVO> getGame(Long gameId) {
    GameEntity entity = gameDao.selectById(gameId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, GameVO.class));
  }

  public ResponseDTO<PageResult<GameVO>> queryGames(GameQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<GameVO> list = gameDao.queryPage(page, form);
    PageResult<GameVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<GameVO> addGame(GameAddForm form) {
    GameEntity entity = SmartBeanUtil.copy(form, GameEntity.class);
    entity.setPlayCount(0L);
    entity.setEnabled(true);
    entity.setDeleted(false);
    gameDao.insert(entity);
    gameCacheManager.evictGameCache(entity.getTenantId());
    return ResponseDTO.ok(SmartBeanUtil.copy(entity, GameVO.class));
  }

  public ResponseDTO<Void> toggleGameEnabled(Long gameId, boolean enabled) {
    GameEntity entity = gameDao.selectById(gameId);
    if (entity == null || entity.getDeleted()) {
      return ResponseDTO.userErrorParam(GameErrorCode.GAME_NOT_FOUND.getMsg());
    }
    entity.setEnabled(enabled);
    gameDao.updateById(entity);
    gameCacheManager.evictGameCache(entity.getTenantId());
    return ResponseDTO.ok();
  }
}
