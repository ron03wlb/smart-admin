package net.lab1024.sa.support.changelog.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.support.changelog.constant.ChangeLogTypeEnum;

/**
 * 系统更新日志 查询
 *
 * @author 卓大
 * @since 2022-09-26 14:53:50 Copyright 1024创新实验室
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChangeLogQueryForm extends PageParam {

  @SchemaEnum(value = ChangeLogTypeEnum.class, desc = "更新类型:[1:特大版本功能更新;2:功能更新;3:bug修复]")
  @CheckEnum(value = ChangeLogTypeEnum.class, message = "更新类型:[1:特大版本功能更新;2:功能更新;3:bug修复] 错误")
  private Integer type;

  @Schema(description = "关键字")
  private String keyword;

  @Schema(description = "发布日期")
  private LocalDate publicDateBegin;

  @Schema(description = "发布日期")
  private LocalDate publicDateEnd;

  @Schema(description = "创建时间")
  private LocalDate createTime;

  @Schema(description = "跳转链接")
  private String link;
}
