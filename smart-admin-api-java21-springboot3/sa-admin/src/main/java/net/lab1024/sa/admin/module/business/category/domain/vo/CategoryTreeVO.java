package net.lab1024.sa.admin.module.business.category.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 类目 层级树 vo
 *
 * @author 1024创新实验室: 胡克
 * @since 2021/08/05 21:26:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class CategoryTreeVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "类目id")
  private Long categoryId;

  @Schema(description = "类目名称")
  private String categoryName;

  @Schema(description = "类目层级全称")
  private String categoryFullName;

  @Schema(description = "父级id")
  private Long parentId;

  @Schema(description = "类目id")
  private Long value;

  @Schema(description = "类目名称")
  private String label;

  @Schema(description = "子类")
  private List<CategoryTreeVO> children;
}
