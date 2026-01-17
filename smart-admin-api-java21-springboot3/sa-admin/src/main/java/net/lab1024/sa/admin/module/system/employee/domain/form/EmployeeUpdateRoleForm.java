package net.lab1024.sa.admin.module.system.employee.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 员工更新角色
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021-12-20 20:55:13 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class EmployeeUpdateRoleForm {

  @Schema(description = "员工id")
  @NotNull(message = "员工id不能为空")
  private Long employeeId;

  @Schema(description = "角色ids")
  @Size(max = 99, message = "角色最多99")
  private List<Long> roleIdList;
}
