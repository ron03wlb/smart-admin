package net.lab1024.sa.support.liteflow.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.constant.LiteFlowConst;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainUpdateForm;
import net.lab1024.sa.support.reload.constant.ReloadConst;
import net.lab1024.sa.support.reload.core.annoation.SmartReload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LiteFlow Chain Manager
 *
 * <p>負責 Chain 的事務管理和緩存管理
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiteFlowChainManager {

  private final LiteFlowChainDao chainDao;
  private final LiteFlowCacheManager cacheManager;
  private final SmartFlowExecutor flowExecutor;

  /**
   * 添加流程
   *
   * @param form 添加表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> add(LiteFlowChainAddForm form, Long userId, String userName) {
    // 1. 校驗 chainCode 唯一性
    LambdaQueryWrapper<LiteFlowChainEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper
        .eq(LiteFlowChainEntity::getChainCode, form.getChainCode())
        .eq(LiteFlowChainEntity::getDeletedFlag, 0);
    Long count = chainDao.selectCount(queryWrapper);
    if (count > 0) {
      return ResponseDTO.userErrorParam("流程編碼已存在");
    }

    // 2. 插入數據庫
    LiteFlowChainEntity entity = new LiteFlowChainEntity();
    entity.setChainName(form.getChainName());
    entity.setChainCode(form.getChainCode());
    entity.setChainType(form.getChainType());
    entity.setChainData(form.getChainData());
    entity.setVersion(1);
    entity.setStatus(LiteFlowConst.STATUS_ENABLED);
    entity.setDeletedFlag(0);
    entity.setRemark(form.getRemark());
    entity.setCreateUserId(userId);
    entity.setCreateUserName(userName);
    entity.setCreateTime(LocalDateTime.now());

    chainDao.insert(entity);

    if (log.isInfoEnabled()) {
      log.info(
          "創建 LiteFlow 流程成功: chainCode={}, chainId={}", form.getChainCode(), entity.getChainId());
    }

    // 3. 緩存刷新
    cacheManager.evictChain(form.getChainCode());

    // 4. 重載引擎
    flowExecutor.reloadRule();

    return ResponseDTO.ok();
  }

  /**
   * 更新流程
   *
   * @param form 更新表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> update(LiteFlowChainUpdateForm form, Long userId, String userName) {
    // 1. 查詢現有記錄
    LiteFlowChainEntity entity = chainDao.selectById(form.getChainId());
    if (entity == null || entity.getDeletedFlag() == 1) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    // 2. 更新數據庫
    entity.setChainName(form.getChainName());
    entity.setChainData(form.getChainData());
    entity.setVersion(entity.getVersion() + 1); // 版本號遞增
    entity.setRemark(form.getRemark());
    entity.setUpdateUserId(userId);
    entity.setUpdateUserName(userName);
    entity.setUpdateTime(LocalDateTime.now());

    chainDao.updateById(entity);

    if (log.isInfoEnabled()) {
      log.info(
          "更新 LiteFlow 流程成功: chainCode={}, version={}", entity.getChainCode(), entity.getVersion());
    }

    // 3. 緩存失效
    cacheManager.evictChain(entity.getChainCode());

    // 4. 重載引擎
    flowExecutor.reloadRule();

    return ResponseDTO.ok();
  }

  /**
   * 刪除流程（軟刪除）
   *
   * @param chainId 流程ID
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<String> delete(Long chainId, Long userId, String userName) {
    // 軟刪除
    LiteFlowChainEntity entity = chainDao.selectById(chainId);
    if (entity == null) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    entity.setDeletedFlag(1);
    entity.setUpdateUserId(userId);
    entity.setUpdateUserName(userName);
    entity.setUpdateTime(LocalDateTime.now());

    chainDao.updateById(entity);

    if (log.isInfoEnabled()) {
      log.info("刪除 LiteFlow 流程成功: chainCode={}", entity.getChainCode());
    }

    // 緩存失效 + 重載
    cacheManager.evictChain(entity.getChainCode());
    flowExecutor.reloadRule();

    return ResponseDTO.ok();
  }

  /**
   * 重載所有流程規則
   *
   * @return 操作結果
   */
  public ResponseDTO<String> reloadAll() {
    if (log.isInfoEnabled()) {
      log.info("手動重載所有 LiteFlow 規則");
    }
    cacheManager.evictAll();
    flowExecutor.reloadRule();
    return ResponseDTO.ok();
  }

  /**
   * SmartReload 集成 - 通過統一 Reload 接口重載 LiteFlow 規則
   *
   * <p>支持通過 POST /reload/execute 接口調用，參數 tag=liteflow
   *
   * @param args 重載參數（可選）
   */
  @SmartReload(ReloadConst.LITEFLOW_RELOAD)
  public void liteflowReload(String args) {
    if (log.isInfoEnabled()) {
      log.info("SmartReload 觸發 LiteFlow 規則重載, args={}", args);
    }
    cacheManager.evictAll();
    flowExecutor.reloadRule();
  }
}
