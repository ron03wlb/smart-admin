package net.lab1024.sa.support.liteflow.core.listener;

import com.yomahub.liteflow.flow.LiteflowResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.support.liteflow.config.LiteFlowProperties;
import net.lab1024.sa.support.liteflow.constant.LiteFlowConst;
import net.lab1024.sa.support.liteflow.dao.LiteFlowExecutionLogDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowExecutionLogEntity;
import net.lab1024.sa.support.liteflow.manager.LiteFlowMetricsManager;
import org.springframework.stereotype.Component;

/**
 * LiteFlow 執行監聽器
 *
 * <p>自動記錄執行日誌和更新指標
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiteFlowExecutionListener {

  private final LiteFlowExecutionLogDao logDao;
  private final LiteFlowMetricsManager metricsManager;
  private final LiteFlowProperties properties;

  private static final ThreadLocal<Long> START_TIME_HOLDER = new ThreadLocal<>();
  private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
  private static final ThreadLocal<Object> INPUT_PARAMS_HOLDER = new ThreadLocal<>();

  /**
   * 執行前處理
   *
   * @param chainCode 流程編碼
   * @param inputParams 輸入參數
   */
  public void beforeExecution(String chainCode, Object inputParams) {
    START_TIME_HOLDER.set(System.currentTimeMillis());
    REQUEST_ID_HOLDER.set(UUID.randomUUID().toString());
    INPUT_PARAMS_HOLDER.set(inputParams);

    log.debug("LiteFlow 執行開始: chainCode={}, requestId={}", chainCode, REQUEST_ID_HOLDER.get());
  }

  /**
   * 執行後處理
   *
   * @param chainCode 流程編碼
   * @param response 執行響應
   */
  public void afterExecution(String chainCode, LiteflowResponse response) {
    try {
      Long startTime = START_TIME_HOLDER.get();
      if (startTime == null) {
        log.warn("執行監聽器未正確初始化: chainCode={}", chainCode);
        return;
      }

      long executionTime = System.currentTimeMillis() - startTime;
      String requestId = REQUEST_ID_HOLDER.get();
      boolean success = response != null && response.isSuccess();

      // 1. 寫入執行日誌
      if (properties.getExecutionLogEnabled()) {
        writeExecutionLog(chainCode, response, executionTime, requestId, success);
      }

      // 2. 更新聚合指標
      if (properties.getMetricsEnabled()) {
        metricsManager.updateMetrics(chainCode, executionTime, success);
      }

    } finally {
      START_TIME_HOLDER.remove();
      REQUEST_ID_HOLDER.remove();
      INPUT_PARAMS_HOLDER.remove();
    }
  }

  /**
   * 寫入執行日誌
   *
   * @param chainCode 流程編碼
   * @param response 執行響應
   * @param executionTime 執行時長
   * @param requestId 請求ID
   * @param success 是否成功
   */
  private void writeExecutionLog(
      String chainCode,
      LiteflowResponse response,
      long executionTime,
      String requestId,
      boolean success) {
    try {
      LiteFlowExecutionLogEntity log = new LiteFlowExecutionLogEntity();
      log.setChainCode(chainCode);
      log.setRequestId(requestId);
      log.setExecutionStatus(
          success ? LiteFlowConst.EXECUTION_SUCCESS : LiteFlowConst.EXECUTION_FAILED);
      log.setExecutionTime((int) executionTime);

      // 輸入參數
      Object inputParams = INPUT_PARAMS_HOLDER.get();
      if (inputParams != null) {
        log.setInputParams(JsonUtil.toJson(inputParams));
      }

      // 輸出結果（暫時不記錄，Slot API 不明確）
      // TODO: 待 LiteFlow 2.15.3 API 確認後完善

      // 錯誤信息
      if (!success && response != null && response.getCause() != null) {
        Exception exception = response.getCause();
        log.setErrorMessage(exception.getMessage());
        log.setErrorStack(getStackTrace(exception));
      }

      log.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
      logDao.insert(log);

      LiteFlowExecutionListener.log.info(
          "LiteFlow 執行日誌已記錄: chainCode={}, requestId={}, success={}, time={}ms",
          chainCode,
          requestId,
          success,
          executionTime);

    } catch (Exception e) {
      LiteFlowExecutionListener.log.error("寫入 LiteFlow 執行日誌失敗", e);
    }
  }

  /**
   * 獲取異常堆棧信息
   *
   * @param e 異常對象
   * @return 堆棧信息字符串
   */
  private String getStackTrace(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
