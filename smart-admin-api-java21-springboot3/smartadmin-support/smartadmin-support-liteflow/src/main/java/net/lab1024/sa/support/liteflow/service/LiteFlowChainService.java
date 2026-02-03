package net.lab1024.sa.support.liteflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowChainVO;
import net.lab1024.sa.support.liteflow.manager.LiteFlowChainManager;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * LiteFlow Chain Service
 *
 * <p>提供 Chain 的業務邏輯
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Service
@RequiredArgsConstructor
public class LiteFlowChainService {

  private final LiteFlowChainDao chainDao;
  private final LiteFlowChainManager chainManager;

  /**
   * 分頁查詢流程
   *
   * @param form 查詢表單
   * @return 分頁結果
   */
  public ResponseDTO<PageResult<LiteFlowChainVO>> queryPage(LiteFlowChainQueryForm form) {
    // Service 層可直接調用 Dao（分頁查詢）
    Page<?> page = SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<LiteFlowChainEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(LiteFlowChainEntity::getDeletedFlag, 0);

    if (StringUtils.isNotBlank(form.getChainName())) {
      queryWrapper.like(LiteFlowChainEntity::getChainName, form.getChainName());
    }
    if (form.getStatus() != null) {
      queryWrapper.eq(LiteFlowChainEntity::getStatus, form.getStatus());
    }

    queryWrapper.orderByDesc(LiteFlowChainEntity::getCreateTime);

    @SuppressWarnings("unchecked")
    Page<LiteFlowChainEntity> resultPage =
        chainDao.selectPage((Page<LiteFlowChainEntity>) page, queryWrapper);
    PageResult<LiteFlowChainVO> pageResult =
        SmartPageUtil.convert2PageResult(page, resultPage.getRecords(), LiteFlowChainVO.class);

    return ResponseDTO.ok(pageResult);
  }

  /**
   * 添加流程
   *
   * @param form 添加表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  public ResponseDTO<String> add(LiteFlowChainAddForm form, Long userId, String userName) {
    // Service 調用 Manager（事務操作）
    return chainManager.add(form, userId, userName);
  }

  /**
   * 更新流程
   *
   * @param form 更新表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  public ResponseDTO<String> update(LiteFlowChainUpdateForm form, Long userId, String userName) {
    return chainManager.update(form, userId, userName);
  }

  /**
   * 刪除流程
   *
   * @param chainId 流程ID
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  public ResponseDTO<String> delete(Long chainId, Long userId, String userName) {
    return chainManager.delete(chainId, userId, userName);
  }

  /**
   * 獲取流程詳情
   *
   * @param chainId 流程ID
   * @return 流程詳情
   */
  public ResponseDTO<LiteFlowChainVO> getDetail(Long chainId) {
    LiteFlowChainEntity entity = chainDao.selectById(chainId);
    if (entity == null || entity.getDeletedFlag() == 1) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    LiteFlowChainVO vo = SmartBeanUtil.copy(entity, LiteFlowChainVO.class);
    return ResponseDTO.ok(vo);
  }

  /**
   * 重載所有流程規則
   *
   * @return 操作結果
   */
  public ResponseDTO<String> reloadAll() {
    return chainManager.reloadAll();
  }
}
