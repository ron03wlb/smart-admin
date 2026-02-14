package net.lab1024.sa.support.liteflow.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * LiteFlow 執行日誌 VO
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow執行日誌VO")
public class LiteFlowExecutionLogVO {

  @Schema(description = "日誌ID")
  private Long logId;

  @Schema(description = "流程編碼")
  private String chainCode;

  @Schema(description = "請求追蹤ID")
  private String requestId;

  @Schema(description = "執行狀態 0-失敗 1-成功")
  private Integer executionStatus;

  @Schema(description = "執行時長（毫秒）")
  private Integer executionTime;

  @Schema(description = "輸入參數（JSON）")
  private String inputParams;

  @Schema(description = "輸出結果（JSON）")
  private String outputResult;

  @Schema(description = "錯誤信息")
  private String errorMessage;

  @Schema(description = "錯誤堆棧")
  private String errorStack;

  @Schema(description = "創建時間")
  private OffsetDateTime createTime;
}
