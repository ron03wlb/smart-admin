package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;

/**
 * LiteFlow 執行日誌查詢表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "LiteFlow執行日誌查詢表單")
public class LiteFlowExecutionLogQueryForm extends PageParam {

  @Schema(description = "流程編碼")
  private String chainCode;

  @Schema(description = "執行狀態 0-失敗 1-成功")
  private Integer executionStatus;

  @Schema(description = "請求追蹤ID")
  private String requestId;
}
