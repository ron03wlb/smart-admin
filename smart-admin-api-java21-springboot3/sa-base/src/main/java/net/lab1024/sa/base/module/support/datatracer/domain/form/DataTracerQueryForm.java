package net.lab1024.sa.base.module.support.datatracer.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.common.swagger.SchemaEnum;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.common.core.domain.PageParam;

/**
 * 查询表单
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-07-23 19:38:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DataTracerQueryForm extends PageParam {

  @SchemaEnum(DataTracerTypeEnum.class)
  private Integer type;

  @Schema(description = "业务id")
  @NotNull(message = "业务id不能为空")
  private Long dataId;

  @Schema(description = "关键字")
  private String keywords;
}
