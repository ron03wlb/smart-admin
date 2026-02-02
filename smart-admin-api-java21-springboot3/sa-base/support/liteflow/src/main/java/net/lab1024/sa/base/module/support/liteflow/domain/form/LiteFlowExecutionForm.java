package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

/**
 * LiteFlow 流程執行表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow流程執行表單")
public class LiteFlowExecutionForm {

  @Schema(description = "流程編碼")
  @NotBlank(message = "流程編碼不能為空")
  private String chainCode;

  @Schema(description = "輸入參數（JSON格式）")
  private Map<String, Object> inputParams;
}
