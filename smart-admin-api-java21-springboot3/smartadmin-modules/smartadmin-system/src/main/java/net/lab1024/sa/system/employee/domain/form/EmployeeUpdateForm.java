package net.lab1024.sa.system.employee.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新员工
 *
 * @author 1024创新实验室: 开云
 * @since 2021-12-20 21:06:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeUpdateForm extends EmployeeAddForm {

  @Schema(description = "员工id")
  @NotNull(message = "员工id不能为空")
  private Long employeeId;
}
