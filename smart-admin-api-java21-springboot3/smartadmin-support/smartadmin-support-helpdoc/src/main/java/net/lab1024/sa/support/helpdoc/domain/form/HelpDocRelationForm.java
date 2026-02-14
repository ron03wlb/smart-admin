package net.lab1024.sa.support.helpdoc.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 帮助文档 关联项目
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class HelpDocRelationForm {

  @Schema(description = "关联名称")
  @NotBlank(message = "关联名称不能为空")
  private String relationName;

  @Schema(description = "关联id")
  @NotNull(message = "关联id不能为空")
  private Long relationId;
}
