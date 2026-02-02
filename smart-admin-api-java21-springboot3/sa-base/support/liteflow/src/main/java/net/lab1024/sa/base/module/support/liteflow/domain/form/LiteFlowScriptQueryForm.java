package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;

/**
 * LiteFlow 腳本查詢表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "LiteFlow腳本查詢表單")
public class LiteFlowScriptQueryForm extends PageParam {

  @Schema(description = "腳本名稱（模糊搜索）")
  private String scriptName;

  @Schema(description = "腳本類型：qlexpress/groovy/javascript")
  private String scriptType;

  @Schema(description = "狀態 0-禁用 1-啟用")
  private Integer status;
}
