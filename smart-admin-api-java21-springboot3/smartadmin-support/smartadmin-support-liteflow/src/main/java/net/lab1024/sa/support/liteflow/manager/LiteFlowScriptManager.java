package net.lab1024.sa.support.liteflow.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.support.liteflow.constant.LiteFlowConst;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.support.liteflow.dao.LiteFlowScriptDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowScriptEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptUpdateForm;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LiteFlow Script Manager
 *
 * <p>負責 Script 的事務管理和緩存管理
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiteFlowScriptManager {

  private final LiteFlowScriptDao scriptDao;
  private final LiteFlowCacheManager cacheManager;
  private final SmartFlowExecutor flowExecutor;

  /**
   * 添加腳本
   *
   * @param form 添加表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> add(LiteFlowScriptAddForm form, Long userId, String userName) {
    // 1. 校驗 scriptCode 唯一性
    LambdaQueryWrapper<LiteFlowScriptEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper
        .eq(LiteFlowScriptEntity::getScriptCode, form.getScriptCode())
        .eq(LiteFlowScriptEntity::getDeletedFlag, 0);
    Long count = scriptDao.selectCount(queryWrapper);
    if (count > 0) {
      return ResponseDTO.userErrorParam("腳本編碼已存在");
    }

    // 2. 插入數據庫
    LiteFlowScriptEntity entity = new LiteFlowScriptEntity();
    entity.setScriptName(form.getScriptName());
    entity.setScriptCode(form.getScriptCode());
    entity.setScriptType(form.getScriptType());
    entity.setScriptData(form.getScriptData());
    entity.setVersion(1);
    entity.setStatus(LiteFlowConst.STATUS_ENABLED);
    entity.setDeletedFlag(0);
    entity.setRemark(form.getRemark());
    entity.setCreateUserId(userId);
    entity.setCreateUserName(userName);
    entity.setCreateTime(LocalDateTime.now());

    scriptDao.insert(entity);

    if (log.isInfoEnabled()) {
      log.info(
          "創建 LiteFlow 腳本成功: scriptCode={}, scriptId={}",
          form.getScriptCode(),
          entity.getScriptId());
    }

    // 3. 緩存刷新
    cacheManager.evictScript(form.getScriptCode());

    // 4. 重載引擎
    flowExecutor.reloadRule();

    return ResponseDTO.ok();
  }

  /**
   * 更新腳本
   *
   * @param form 更新表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> update(LiteFlowScriptUpdateForm form, Long userId, String userName) {
    // 1. 查詢現有記錄
    LiteFlowScriptEntity entity = scriptDao.selectById(form.getScriptId());
    if (entity == null || entity.getDeletedFlag() == 1) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    // 2. 更新數據庫
    entity.setScriptName(form.getScriptName());
    entity.setScriptData(form.getScriptData());
    entity.setVersion(entity.getVersion() + 1); // 版本號遞增
    entity.setRemark(form.getRemark());
    entity.setUpdateUserId(userId);
    entity.setUpdateUserName(userName);
    entity.setUpdateTime(LocalDateTime.now());

    scriptDao.updateById(entity);

    if (log.isInfoEnabled()) {
      log.info(
          "更新 LiteFlow 腳本成功: scriptCode={}, version={}",
          entity.getScriptCode(),
          entity.getVersion());
    }

    // 3. 緩存失效
    cacheManager.evictScript(entity.getScriptCode());

    // 4. 重載引擎
    flowExecutor.reloadRule();

    return ResponseDTO.ok();
  }

  /**
   * 刪除腳本（軟刪除）
   *
   * @param scriptId 腳本ID
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> delete(Long scriptId, Long userId, String userName) {
    // 軟刪除
    LiteFlowScriptEntity entity = scriptDao.selectById(scriptId);
    if (entity == null) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    entity.setDeletedFlag(1);
    entity.setUpdateUserId(userId);
    entity.setUpdateUserName(userName);
    entity.setUpdateTime(LocalDateTime.now());

    scriptDao.updateById(entity);

    if (log.isInfoEnabled()) {
      log.info("刪除 LiteFlow 腳本成功: scriptCode={}", entity.getScriptCode());
    }

    // 緩存失效 + 重載
    cacheManager.evictScript(entity.getScriptCode());
    flowExecutor.reloadRule();

    return ResponseDTO.ok();
  }
}
