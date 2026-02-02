package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * LiteFlow 腳本更新表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow腳本更新表單")
public class LiteFlowScriptUpdateForm {

  @Schema(description = "腳本ID")
  @NotNull(message = "腳本ID不能為空")
  private Long scriptId;

  @Schema(description = "腳本名稱")
  @NotBlank(message = "腳本名稱不能為空")
  private String scriptName;

  @Schema(description = "腳本內容（代碼）")
  @NotBlank(message = "腳本內容不能為空")
  private String scriptData;

  @Schema(description = "備註")
  private String remark;
}
