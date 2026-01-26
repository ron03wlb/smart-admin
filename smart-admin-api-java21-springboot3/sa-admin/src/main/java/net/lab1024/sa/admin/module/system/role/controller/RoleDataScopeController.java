package net.lab1024.sa.admin.module.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleDataScopeUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleDataScopeVO;
import net.lab1024.sa.admin.module.system.role.service.RoleDataScopeService;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色的数据权限配置
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-02-26 22:09:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE_DATA_SCOPE)
public class RoleDataScopeController {

  private final RoleDataScopeService roleDataScopeService;

  @Operation(summary = "获取某角色所设置的数据范围 @author 卓大")
  @GetMapping("/role/dataScope/getRoleDataScopeList/{roleId}")
  public ResponseDTO<List<RoleDataScopeVO>> dataScopeListByRole(@PathVariable Long roleId) {
    return roleDataScopeService.getRoleDataScopeList(roleId);
  }

  @Operation(summary = "批量设置某角色数据范围 @author 卓大")
  @PostMapping("/role/dataScope/updateRoleDataScopeList")
  @SaCheckPermission("system:role:dataScope:update")
  public ResponseDTO<String> updateRoleDataScopeList(
      @RequestBody @Valid RoleDataScopeUpdateForm roleDataScopeUpdateForm) {
    return roleDataScopeService.updateRoleDataScopeList(roleDataScopeUpdateForm);
  }
}
