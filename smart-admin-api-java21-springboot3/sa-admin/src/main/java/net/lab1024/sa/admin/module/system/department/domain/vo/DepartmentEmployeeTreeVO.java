package net.lab1024.sa.admin.module.system.department.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;

/**
 * 部门
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-12 20:37:48 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class DepartmentEmployeeTreeVO extends DepartmentVO {

  private static final long serialVersionUID = 1L;

  @Schema(description = "部门员工列表")
  private List<EmployeeVO> employees;

  @Schema(description = "子部门")
  private List<DepartmentEmployeeTreeVO> children;
}
