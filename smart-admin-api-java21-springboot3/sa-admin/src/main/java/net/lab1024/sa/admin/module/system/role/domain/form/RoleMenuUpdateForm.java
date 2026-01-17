package net.lab1024.sa.admin.module.system.role.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 角色的菜单更新
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class RoleMenuUpdateForm {

  /** 角色id */
  @Schema(description = "角色id")
  @NotNull(message = "角色id不能为空")
  private Long roleId;

  /** 菜单ID 集合 */
  @Schema(description = "菜单ID集合")
  @NotNull(message = "菜单ID不能为空")
  private List<Long> menuIdList;
}
