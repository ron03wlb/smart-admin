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
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import net.lab1024.sa.igaming.game.domain.form.GameProviderAddForm;
import net.lab1024.sa.igaming.game.domain.form.GameProviderQueryForm;
import net.lab1024.sa.igaming.game.domain.form.GameProviderUpdateForm;
import net.lab1024.sa.igaming.game.domain.vo.GameProviderVO;
import net.lab1024.sa.igaming.game.manager.GameCacheManager;
import org.springframework.stereotype.Service;

/**
 * Game provider service — CRUD for GP configuration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class GameProviderService {

  private final GameProviderDao gameProviderDao;
  private final GameCacheManager gameCacheManager;

  public Option<GameProviderVO> getProvider(Long providerId) {
    GameProviderEntity entity = gameProviderDao.selectById(providerId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, GameProviderVO.class));
  }

  public ResponseDTO<PageResult<GameProviderVO>> queryProviders(GameProviderQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<GameProviderVO> list = gameProviderDao.queryPage(page, form);
    PageResult<GameProviderVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<GameProviderVO> addProvider(GameProviderAddForm form) {
    GameProviderEntity entity = SmartBeanUtil.copy(form, GameProviderEntity.class);
    entity.setEncryptedApiKey(form.getApiKey());
    entity.setEnabled(true);
    entity.setHealthStatus(1);
    entity.setDeleted(false);
    gameProviderDao.insert(entity);
    return ResponseDTO.ok(SmartBeanUtil.copy(entity, GameProviderVO.class));
  }

  public ResponseDTO<Void> updateProvider(GameProviderUpdateForm form) {
    GameProviderEntity entity = gameProviderDao.selectById(form.getProviderId());
    if (entity == null || entity.getDeleted()) {
      return ResponseDTO.userErrorParam(GameErrorCode.PROVIDER_NOT_FOUND.getMsg());
    }
    if (form.getProviderName() != null) {
      entity.setProviderName(form.getProviderName());
    }
    if (form.getApiUrl() != null) {
      entity.setApiUrl(form.getApiUrl());
    }
    if (form.getApiKey() != null) {
      entity.setEncryptedApiKey(form.getApiKey());
    }
    if (form.getCallbackUrl() != null) {
      entity.setCallbackUrl(form.getCallbackUrl());
    }
    if (form.getEnabled() != null) {
      entity.setEnabled(form.getEnabled());
    }
    gameProviderDao.updateById(entity);
    gameCacheManager.evictGameCache(entity.getTenantId());
    return ResponseDTO.ok();
  }

  public ResponseDTO<Void> enableProvider(Long providerId) {
    GameProviderEntity entity = gameProviderDao.selectById(providerId);
    if (entity == null || entity.getDeleted()) {
      return ResponseDTO.userErrorParam(GameErrorCode.PROVIDER_NOT_FOUND.getMsg());
    }
    entity.setEnabled(true);
    gameProviderDao.updateById(entity);
    gameCacheManager.evictGameCache(entity.getTenantId());
    return ResponseDTO.ok();
  }

  public ResponseDTO<Void> disableProvider(Long providerId) {
    GameProviderEntity entity = gameProviderDao.selectById(providerId);
    if (entity == null || entity.getDeleted()) {
      return ResponseDTO.userErrorParam(GameErrorCode.PROVIDER_NOT_FOUND.getMsg());
    }
    entity.setEnabled(false);
    gameProviderDao.updateById(entity);
    gameCacheManager.evictGameCache(entity.getTenantId());
    return ResponseDTO.ok();
  }
}
