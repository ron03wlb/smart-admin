package net.lab1024.sa.base.module.support.liteflow.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * LiteFlow 執行結果 VO
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow執行結果VO")
public class LiteFlowExecutionResultVO {

  @Schema(description = "流程編碼")
  private String chainCode;

  @Schema(description = "執行是否成功")
  private Boolean success;

  @Schema(description = "執行時長（毫秒）")
  private Long executionTime;

  @Schema(description = "輸出結果")
  private Object outputResult;

  @Schema(description = "錯誤信息")
  private String errorMessage;
}
