package net.lab1024.sa.system.employee.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改登录人头像
 *
 * @author 1024创新实验室: 善逸
 * @since 2024年6月30日00:26:35 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class EmployeeUpdateAvatarForm {

  @Schema(hidden = true)
  private Long employeeId;

  @Schema(description = "头像")
  @NotBlank(message = "头像不能为空哦")
  private String avatar;
}
