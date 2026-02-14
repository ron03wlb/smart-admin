package net.lab1024.sa.system.employee.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 员工更新部门
 *
 * @author 1024创新实验室: 开云
 * @since 2021-12-20 21:06:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class EmployeeBatchUpdateDepartmentForm {

  @Schema(description = "员工id")
  @NotEmpty(message = "员工id不能为空")
  @Size(max = 99, message = "一次最多调整99个员工")
  private List<Long> employeeIdList;

  @Schema(description = "部门ID")
  @NotNull(message = "部门ID不能为空")
  private Long departmentId;
}
