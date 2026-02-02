package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * LiteFlow 腳本添加表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow腳本添加表單")
public class LiteFlowScriptAddForm {

  @Schema(description = "腳本名稱")
  @NotBlank(message = "腳本名稱不能為空")
  private String scriptName;

  @Schema(description = "腳本編碼")
  @NotBlank(message = "腳本編碼不能為空")
  @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "腳本編碼只能包含字母、數字、下劃線和連字符")
  private String scriptCode;

  @Schema(description = "腳本類型：qlexpress/groovy/javascript")
  @NotBlank(message = "腳本類型不能為空")
  private String scriptType;

  @Schema(description = "腳本內容（代碼）")
  @NotBlank(message = "腳本內容不能為空")
  private String scriptData;

  @Schema(description = "備註")
  private String remark;
}
