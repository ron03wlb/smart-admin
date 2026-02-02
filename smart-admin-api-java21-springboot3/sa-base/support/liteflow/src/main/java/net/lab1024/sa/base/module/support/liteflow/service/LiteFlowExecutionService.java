package net.lab1024.sa.base.module.support.liteflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.liteflow.constant.LiteFlowConst;
import net.lab1024.sa.base.module.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.base.module.support.liteflow.core.listener.LiteFlowExecutionListener;
import net.lab1024.sa.base.module.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.base.module.support.liteflow.dao.LiteFlowExecutionLogDao;
import net.lab1024.sa.base.module.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.base.module.support.liteflow.domain.entity.LiteFlowExecutionLogEntity;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowExecutionForm;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowExecutionLogQueryForm;
import net.lab1024.sa.base.module.support.liteflow.domain.vo.LiteFlowExecutionLogVO;
import net.lab1024.sa.base.module.support.liteflow.domain.vo.LiteFlowExecutionResultVO;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * LiteFlow 執行服務
 *
 * <p>提供流程執行和日誌查詢功能
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiteFlowExecutionService {

  private final SmartFlowExecutor flowExecutor;
  private final LiteFlowChainDao chainDao;
  private final LiteFlowExecutionLogDao logDao;
  private final LiteFlowExecutionListener executionListener;

  /**
   * 執行流程
   *
   * @param form 執行表單
   * @return 執行結果
   */
  public ResponseDTO<LiteFlowExecutionResultVO> execute(LiteFlowExecutionForm form) {
    // 1. 校驗 Chain 是否存在
    LambdaQueryWrapper<LiteFlowChainEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper
        .eq(LiteFlowChainEntity::getChainCode, form.getChainCode())
        .eq(LiteFlowChainEntity::getStatus, LiteFlowConst.STATUS_ENABLED)
        .eq(LiteFlowChainEntity::getDeletedFlag, 0);

    LiteFlowChainEntity chain = chainDao.selectOne(queryWrapper);
    if (chain == null) {
      return ResponseDTO.userErrorParam("流程不存在或已禁用");
    }

    // 2. 執行前回調
    executionListener.beforeExecution(form.getChainCode(), form.getInputParams());

    try {
      // 3. 執行流程
      LiteflowResponse response = flowExecutor.execute(form.getChainCode(), form.getInputParams());

      // 4. 執行後回調
      executionListener.afterExecution(form.getChainCode(), response);

      // 5. 構建結果 VO
      LiteFlowExecutionResultVO resultVO = new LiteFlowExecutionResultVO();
      resultVO.setChainCode(form.getChainCode());
      resultVO.setSuccess(response.isSuccess());
      // TODO: 待 LiteFlow API 確認後完善執行時間
      resultVO.setExecutionTime(0L);

      if (response.isSuccess()) {
        // TODO: 待 Slot API 確認後完善輸出結果
        resultVO.setOutputResult(null);
      } else {
        Exception cause = response.getCause();
        resultVO.setErrorMessage(cause != null ? cause.getMessage() : "執行失敗");
      }

      return ResponseDTO.ok(resultVO);

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("執行 LiteFlow 流程異常: chainCode={}", form.getChainCode(), e);
      }
      return ResponseDTO.userErrorParam("流程執行異常: " + e.getMessage());
    }
  }

  /**
   * 分頁查詢執行日誌
   *
   * @param form 查詢表單
   * @return 分頁結果
   */
  public ResponseDTO<PageResult<LiteFlowExecutionLogVO>> queryLog(
      LiteFlowExecutionLogQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<LiteFlowExecutionLogEntity> queryWrapper = new LambdaQueryWrapper<>();

    if (StringUtils.isNotBlank(form.getChainCode())) {
      queryWrapper.eq(LiteFlowExecutionLogEntity::getChainCode, form.getChainCode());
    }
    if (form.getExecutionStatus() != null) {
      queryWrapper.eq(LiteFlowExecutionLogEntity::getExecutionStatus, form.getExecutionStatus());
    }
    if (StringUtils.isNotBlank(form.getRequestId())) {
      queryWrapper.eq(LiteFlowExecutionLogEntity::getRequestId, form.getRequestId());
    }

    queryWrapper.orderByDesc(LiteFlowExecutionLogEntity::getCreateTime);

    @SuppressWarnings("unchecked")
    Page<LiteFlowExecutionLogEntity> resultPage =
        logDao.selectPage((Page<LiteFlowExecutionLogEntity>) page, queryWrapper);
    PageResult<LiteFlowExecutionLogVO> pageResult =
        SmartPageUtil.convert2PageResult(
            page, resultPage.getRecords(), LiteFlowExecutionLogVO.class);

    return ResponseDTO.ok(pageResult);
  }

  /**
   * 獲取執行日誌詳情
   *
   * @param logId 日誌ID
   * @return 日誌詳情
   */
  public ResponseDTO<LiteFlowExecutionLogVO> getLogDetail(Long logId) {
    LiteFlowExecutionLogEntity entity = logDao.selectById(logId);
    if (entity == null) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    LiteFlowExecutionLogVO vo = SmartBeanUtil.copy(entity, LiteFlowExecutionLogVO.class);
    return ResponseDTO.ok(vo);
  }
}
