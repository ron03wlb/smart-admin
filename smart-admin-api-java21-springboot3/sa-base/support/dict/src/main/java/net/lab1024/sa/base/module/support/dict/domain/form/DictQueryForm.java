package net.lab1024.sa.base.module.support.dict.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;

/**
 * 数据字典 分页查询表单
 *
 * @author 1024创新实验室-主任-卓大
 * @since 2025-03-25 22:25:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictQueryForm extends PageParam {

  @Schema(description = "关键字")
  private String keywords;

  @Schema(description = "禁用状态")
  private Boolean disabledFlag;
}
