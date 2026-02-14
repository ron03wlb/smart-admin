package net.lab1024.sa.support.codegenerator.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代码生成 预览 表单
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/6/23 23:20:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class CodeGeneratorPreviewForm {

  @NotBlank(message = "模板文件 不能为空")
  @Schema(description = "模板文件")
  private String templateFile;

  @NotBlank(message = "表名 不能为空")
  @Schema(description = "表名")
  private String tableName;
}
