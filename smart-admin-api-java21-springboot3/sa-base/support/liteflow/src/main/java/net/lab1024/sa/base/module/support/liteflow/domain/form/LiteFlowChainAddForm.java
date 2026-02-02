package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * LiteFlow 流程添加表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow流程添加表單")
public class LiteFlowChainAddForm {

  @Schema(description = "流程名稱")
  @NotBlank(message = "流程名稱不能為空")
  private String chainName;

  @Schema(description = "流程編碼")
  @NotBlank(message = "流程編碼不能為空")
  @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "流程編碼只能包含字母、數字、下劃線和連字符")
  private String chainCode;

  @Schema(description = "流程類型 1-普通 2-條件 3-循環")
  private Integer chainType = 1;

  @Schema(description = "流程定義（EL表達式）")
  @NotBlank(message = "流程定義不能為空")
  private String chainData;

  @Schema(description = "備註")
  private String remark;
}
