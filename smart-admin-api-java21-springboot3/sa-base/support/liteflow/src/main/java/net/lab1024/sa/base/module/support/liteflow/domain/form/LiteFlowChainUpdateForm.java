package net.lab1024.sa.base.module.support.liteflow.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * LiteFlow 流程更新表單
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow流程更新表單")
public class LiteFlowChainUpdateForm {

  @Schema(description = "流程ID")
  @NotNull(message = "流程ID不能為空")
  private Long chainId;

  @Schema(description = "流程名稱")
  @NotBlank(message = "流程名稱不能為空")
  private String chainName;

  @Schema(description = "流程定義（EL表達式）")
  @NotBlank(message = "流程定義不能為空")
  private String chainData;

  @Schema(description = "備註")
  private String remark;
}
