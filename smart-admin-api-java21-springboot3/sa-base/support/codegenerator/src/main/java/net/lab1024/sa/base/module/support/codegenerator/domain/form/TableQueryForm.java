package net.lab1024.sa.base.module.support.codegenerator.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.PageParam;

/**
 * 查询表数据
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-06-30 22:15:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TableQueryForm extends PageParam {

  @Schema(description = "表名关键字")
  private String tableNameKeywords;
}
