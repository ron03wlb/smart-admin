package net.lab1024.sa.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.system.role.domain.form.RoleAddForm;
import net.lab1024.sa.system.role.domain.form.RoleUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleVO;
import net.lab1024.sa.system.role.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-12-14 19:40:28 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE)
public class RoleController {

  private final RoleService roleService;

  @Operation(summary = "添加角色 @author 卓大")
  @PostMapping("/role/add")
  @SaCheckPermission("system:role:add")
  public ResponseDTO<String> addRole(@Valid @RequestBody RoleAddForm roleAddForm) {
    return roleService.addRole(roleAddForm);
  }

  @Operation(summary = "删除角色 @author 卓大")
  @GetMapping("/role/delete/{roleId}")
  @SaCheckPermission("system:role:delete")
  public ResponseDTO<String> deleteRole(@PathVariable Long roleId) {
    return roleService.deleteRole(roleId);
  }

  @Operation(summary = "更新角色 @author 卓大")
  @PostMapping("/role/update")
  @SaCheckPermission("system:role:update")
  public ResponseDTO<String> updateRole(@Valid @RequestBody RoleUpdateForm roleUpdateDTO) {
    return roleService.updateRole(roleUpdateDTO);
  }

  @Operation(summary = "获取角色数据 @author 卓大")
  @GetMapping("/role/get/{roleId}")
  public ResponseDTO<RoleVO> getRole(@PathVariable("roleId") Long roleId) {
    return roleService.getRoleById(roleId);
  }

  @Operation(summary = "获取所有角色 @author 卓大")
  @GetMapping("/role/getAll")
  public ResponseDTO<List<RoleVO>> getAllRole() {
    return roleService.getAllRole();
  }
}
