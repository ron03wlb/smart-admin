package net.lab1024.sa.support.helpdoc.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 帮助文档
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressWarnings("PMD.LongVariable")
public class HelpDocVO {

  @Schema(description = "id")
  private Long helpDocId;

  @Schema(description = "标题")
  private String title;

  @Schema(description = "分类")
  private Long helpDocCatalogId;

  @Schema(description = "分类名称")
  private String helpDocCatalogName;

  @Schema(description = "作者")
  private String author;

  @Schema(description = "排序")
  private Integer sort;

  @Schema(description = "页面浏览量")
  private Integer pageViewCount;

  @Schema(description = "用户浏览量")
  private Integer userViewCount;

  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  @Schema(description = "更新时间")
  private LocalDateTime updateTime;
}
