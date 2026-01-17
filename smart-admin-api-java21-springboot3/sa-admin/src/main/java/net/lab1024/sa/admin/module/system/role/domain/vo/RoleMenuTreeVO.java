package net.lab1024.sa.admin.module.system.role.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuSimpleTreeVO;

/**
 * 角色菜单树
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class RoleMenuTreeVO {

  @Schema(description = "角色ID")
  private Long roleId;

  @Schema(description = "菜单列表")
  private List<MenuSimpleTreeVO> menuTreeList;

  @Schema(description = "选中的菜单ID")
  private List<Long> selectedMenuId;
}
