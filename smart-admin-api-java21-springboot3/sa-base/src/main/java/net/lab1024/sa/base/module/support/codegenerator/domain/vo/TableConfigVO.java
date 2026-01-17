package net.lab1024.sa.base.module.support.codegenerator.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeBasic;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeDelete;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeField;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdate;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeQueryField;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeTableField;

/**
 * 表的配置信息
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2022/9/21 21:07:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableConfigVO {

  @Schema(description = "基础命名信息")
  private CodeBasic basic;

  @Schema(description = "字段列")
  private List<CodeField> fields;

  @Schema(description = "增加、修改 信息")
  private CodeInsertAndUpdate insertAndUpdate;

  @Schema(description = "删除 信息")
  private CodeDelete deleteInfo;

  @Schema(description = "查询字段")
  private List<CodeQueryField> queryFields;

  @Schema(description = "列表字段")
  private List<CodeTableField> tableFields;
}
