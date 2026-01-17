package net.lab1024.sa.admin.module.system.department.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-12 20:37:48 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class DepartmentTreeVO extends DepartmentVO {

  private static final long serialVersionUID = 1L;

  @Schema(description = "同级上一个元素id")
  private Long preId;

  @Schema(description = "同级下一个元素id")
  private Long nextId;

  @Schema(description = "子部门")
  private List<DepartmentTreeVO> children;

  @Schema(description = "自己和所有递归子部门的id集合")
  private List<Long> selfAndAllChildrenIdList;
}
