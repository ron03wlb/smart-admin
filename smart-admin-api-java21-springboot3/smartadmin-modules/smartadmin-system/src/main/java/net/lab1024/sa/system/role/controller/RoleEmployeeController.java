package net.lab1024.sa.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.system.role.domain.form.RoleEmployeeQueryForm;
import net.lab1024.sa.system.role.domain.form.RoleEmployeeUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleSelectedVO;
import net.lab1024.sa.system.role.service.RoleEmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色的员工
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-02-26 22:09:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE_EMPLOYEE)
public class RoleEmployeeController {

  private final RoleEmployeeService roleEmployeeService;

  @Operation(summary = "查询某个角色下的员工列表  @author 卓大")
  @PostMapping("/role/employee/queryEmployee")
  public ResponseDTO<PageResult<EmployeeVO>> queryEmployee(
      @Valid @RequestBody RoleEmployeeQueryForm roleEmployeeQueryForm) {
    return roleEmployeeService.queryEmployee(roleEmployeeQueryForm);
  }

  @Operation(summary = "获取某个角色下的所有员工列表(无分页)  @author 卓大")
  @GetMapping("/role/employee/getAllEmployeeByRoleId/{roleId}")
  public ResponseDTO<List<EmployeeVO>> listAllEmployeeRoleId(@PathVariable Long roleId) {
    return ResponseDTO.ok(roleEmployeeService.getAllEmployeeByRoleId(roleId));
  }

  @Operation(summary = "从角色成员列表中移除员工 @author 卓大")
  @GetMapping("/role/employee/removeEmployee")
  @SaCheckPermission("system:role:employee:delete")
  public ResponseDTO<String> removeEmployee(Long employeeId, Long roleId) {
    return roleEmployeeService.removeRoleEmployee(employeeId, roleId);
  }

  @Operation(summary = "从角色成员列表中批量移除员工 @author 卓大")
  @PostMapping("/role/employee/batchRemoveRoleEmployee")
  @SaCheckPermission("system:role:employee:batch:delete")
  public ResponseDTO<String> batchRemoveEmployee(
      @Valid @RequestBody RoleEmployeeUpdateForm updateForm) {
    return roleEmployeeService.batchRemoveRoleEmployee(updateForm);
  }

  @Operation(summary = "角色成员列表中批量添加员工 @author 卓大")
  @PostMapping("/role/employee/batchAddRoleEmployee")
  @SaCheckPermission("system:role:employee:add")
  public ResponseDTO<String> addEmployeeList(@Valid @RequestBody RoleEmployeeUpdateForm addForm) {
    return roleEmployeeService.batchAddRoleEmployee(addForm);
  }

  @Operation(summary = "获取员工所有选中的角色和所有角色 @author 卓大")
  @GetMapping("/role/employee/getRoles/{employeeId}")
  public ResponseDTO<List<RoleSelectedVO>> getRoleByEmployeeId(@PathVariable Long employeeId) {
    return ResponseDTO.ok(roleEmployeeService.getRoleInfoListByEmployeeId(employeeId));
  }
}
