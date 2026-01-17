package net.lab1024.sa.base.module.support.helpdoc.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.common.domain.PageParam;

/**
 * 帮助文档 分页查询
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HelpDocQueryForm extends PageParam {

  @Schema(description = "分类")
  private Long helpDocCatalogId;

  @Schema(description = "标题")
  private String keywords;

  @Schema(description = "创建-开始时间")
  private LocalDate createTimeBegin;

  @Schema(description = "创建-截止时间")
  private LocalDate createTimeEnd;
}
