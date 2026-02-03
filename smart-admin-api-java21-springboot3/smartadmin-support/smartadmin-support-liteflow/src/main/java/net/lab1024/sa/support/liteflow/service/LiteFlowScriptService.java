package net.lab1024.sa.support.liteflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.support.liteflow.dao.LiteFlowScriptDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowScriptEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowScriptVO;
import net.lab1024.sa.support.liteflow.manager.LiteFlowScriptManager;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * LiteFlow Script Service
 *
 * <p>提供 Script 的業務邏輯
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Service
@RequiredArgsConstructor
public class LiteFlowScriptService {

  private final LiteFlowScriptDao scriptDao;
  private final LiteFlowScriptManager scriptManager;

  /**
   * 分頁查詢腳本
   *
   * @param form 查詢表單
   * @return 分頁結果
   */
  public ResponseDTO<PageResult<LiteFlowScriptVO>> queryPage(LiteFlowScriptQueryForm form) {
    // Service 層可直接調用 Dao（分頁查詢）
    Page<?> page = SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<LiteFlowScriptEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(LiteFlowScriptEntity::getDeletedFlag, 0);

    if (StringUtils.isNotBlank(form.getScriptName())) {
      queryWrapper.like(LiteFlowScriptEntity::getScriptName, form.getScriptName());
    }
    if (StringUtils.isNotBlank(form.getScriptType())) {
      queryWrapper.eq(LiteFlowScriptEntity::getScriptType, form.getScriptType());
    }
    if (form.getStatus() != null) {
      queryWrapper.eq(LiteFlowScriptEntity::getStatus, form.getStatus());
    }

    queryWrapper.orderByDesc(LiteFlowScriptEntity::getCreateTime);

    @SuppressWarnings("unchecked")
    Page<LiteFlowScriptEntity> resultPage =
        scriptDao.selectPage((Page<LiteFlowScriptEntity>) page, queryWrapper);
    PageResult<LiteFlowScriptVO> pageResult =
        SmartPageUtil.convert2PageResult(page, resultPage.getRecords(), LiteFlowScriptVO.class);

    return ResponseDTO.ok(pageResult);
  }

  /**
   * 添加腳本
   *
   * @param form 添加表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  public ResponseDTO<String> add(LiteFlowScriptAddForm form, Long userId, String userName) {
    // Service 調用 Manager（事務操作）
    return scriptManager.add(form, userId, userName);
  }

  /**
   * 更新腳本
   *
   * @param form 更新表單
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  public ResponseDTO<String> update(LiteFlowScriptUpdateForm form, Long userId, String userName) {
    return scriptManager.update(form, userId, userName);
  }

  /**
   * 刪除腳本
   *
   * @param scriptId 腳本ID
   * @param userId 用戶ID
   * @param userName 用戶名
   * @return 操作結果
   */
  public ResponseDTO<String> delete(Long scriptId, Long userId, String userName) {
    return scriptManager.delete(scriptId, userId, userName);
  }

  /**
   * 獲取腳本詳情
   *
   * @param scriptId 腳本ID
   * @return 腳本詳情
   */
  public ResponseDTO<LiteFlowScriptVO> getDetail(Long scriptId) {
    LiteFlowScriptEntity entity = scriptDao.selectById(scriptId);
    if (entity == null || entity.getDeletedFlag() == 1) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    LiteFlowScriptVO vo = SmartBeanUtil.copy(entity, LiteFlowScriptVO.class);
    return ResponseDTO.ok(vo);
  }
}
