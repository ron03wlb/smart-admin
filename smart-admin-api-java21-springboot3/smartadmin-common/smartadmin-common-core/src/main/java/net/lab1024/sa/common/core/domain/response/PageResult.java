package net.lab1024.sa.common.core.domain.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * 分页返回对象
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2020/04/28 16:19 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class PageResult<T> {

  /** 当前页 */
  @Schema(description = "当前页")
  private Long pageNum;

  /** 每页的数量 */
  @Schema(description = "每页的数量")
  private Long pageSize;

  /** 总记录数 */
  @Schema(description = "总记录数")
  private Long total;

  /** 总页数 */
  @Schema(description = "总页数")
  private Long pages;

  /** 结果集 */
  @Schema(description = "结果集")
  private List<T> list;

  @Schema(description = "是否为空")
  private Boolean emptyFlag;
}
