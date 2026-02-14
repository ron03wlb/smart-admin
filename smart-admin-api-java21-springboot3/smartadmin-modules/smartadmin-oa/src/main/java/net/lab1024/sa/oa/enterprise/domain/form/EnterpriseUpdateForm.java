package net.lab1024.sa.oa.enterprise.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OA企业模块编辑
 *
 * @author 1024创新实验室: 开云
 * @since 2022/7/28 20:37:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EnterpriseUpdateForm extends EnterpriseCreateForm {

  @Schema(description = "企业ID")
  @NotNull(message = "企业ID不能为空")
  private Long enterpriseId;
}
