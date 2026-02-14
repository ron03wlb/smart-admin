package net.lab1024.sa.support.helpdoc.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 帮助文档 关联项目
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class HelpDocRelationVO {

  @Schema(description = "关联名称")
  private String relationName;

  @Schema(description = "关联id")
  private Long relationId;
}
