package net.lab1024.sa.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.system.role.domain.form.RoleMenuUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleMenuTreeVO;
import net.lab1024.sa.system.role.service.RoleMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色的菜单
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-02-26 21:34:01 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE_MENU)
public class RoleMenuController {

  private final RoleMenuService roleMenuService;

  @Operation(summary = "更新角色权限 @author 卓大")
  @PostMapping("/role/menu/updateRoleMenu")
  @SaCheckPermission("system:role:menu:update")
  public ResponseDTO<String> updateRoleMenu(@Valid @RequestBody RoleMenuUpdateForm updateDTO) {
    return roleMenuService.updateRoleMenu(updateDTO);
  }

  @Operation(summary = "获取角色关联菜单权限 @author 卓大")
  @GetMapping("/role/menu/getRoleSelectedMenu/{roleId}")
  public ResponseDTO<RoleMenuTreeVO> getRoleSelectedMenu(@PathVariable Long roleId) {
    return roleMenuService.getRoleSelectedMenu(roleId);
  }
}
