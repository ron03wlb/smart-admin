package net.lab1024.sa.system.role.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Data;

/**
 * 角色的员工更新
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class RoleEmployeeUpdateForm {

  @Schema(description = "角色id")
  @NotNull(message = "角色id不能为空")
  protected Long roleId;

  @Schema(description = "员工id集合")
  @NotEmpty(message = "员工id不能为空")
  protected Set<Long> employeeIdList;
}
