package net.lab1024.sa.base.module.support.codegenerator.domain.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import net.lab1024.sa.base.module.support.codegenerator.constant.CodeGeneratorPageTypeEnum;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;
import net.lab1024.sa.foundation.validation.annotation.CheckEnum;

/**
 * 代码生成 增加、修改 模型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-06-30 22:15:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class CodeInsertAndUpdate {

  @NotNull(message = "3.增加、修改 是否支持增加、修改 不能为空")
  private Boolean isSupportInsertAndUpdate;

  @SchemaEnum(CodeGeneratorPageTypeEnum.class)
  @CheckEnum(value = CodeGeneratorPageTypeEnum.class, message = "3.增加、修改 增加、修改 页面类型 枚举值错误")
  private String pageType;

  @Schema(description = "宽度")
  private String width;

  @NotNull(message = "3.增加、修改 每行字段数量 不能为空")
  @Schema(description = "每行字段数量")
  private Integer countPerLine;

  @Schema(description = "字段列表")
  private List<CodeInsertAndUpdateField> fieldList;
}
