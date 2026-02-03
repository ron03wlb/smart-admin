package net.lab1024.sa.support.liteflow.core.executor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SmartAdmin LiteFlow 流程執行包裝器
 *
 * <p>提供統一的流程執行入口，封裝 LiteFlow FlowExecutor
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartFlowExecutor {

  private final FlowExecutor flowExecutor;

  /**
   * 執行流程（同步）
   *
   * @param chainCode 流程編碼
   * @param params 輸入參數
   * @return 流程執行結果
   */
  public LiteflowResponse execute(String chainCode, Object... params) {
    try {
      if (log.isInfoEnabled()) {
        log.info("執行 LiteFlow 流程: chainCode={}", chainCode);
      }
      LiteflowResponse response = flowExecutor.execute2Resp(chainCode, null, params);

      if (response.isSuccess()) {
        if (log.isInfoEnabled()) {
          log.info(
              "流程執行成功: chainCode={}, 執行步驟={}", chainCode, response.getExecuteStepStrWithTime());
        }
      } else {
        if (log.isErrorEnabled()) {
          Exception cause = response.getCause();
          log.error(
              "流程執行失敗: chainCode={}, 錯誤={}",
              chainCode,
              cause != null ? cause.getMessage() : "Unknown error");
        }
      }

      return response;

    } catch (Exception e) {
      log.error("流程執行異常: chainCode={}", chainCode, e);
      throw e;
    }
  }

  /**
   * 重新加載流程規則
   *
   * <p>從數據庫重新加載所有流程定義和腳本節點
   */
  public void reloadRule() {
    log.info("重新加載 LiteFlow 規則");
    flowExecutor.reloadRule();
  }
}
