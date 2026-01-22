package net.lab1024.sa.admin.module.system.employee.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeBatchUpdateDepartmentForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeQueryForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateAvatarForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateCenterForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdatePasswordForm;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.admin.module.system.employee.service.EmployeeService;
import net.lab1024.sa.base.web.util.SmartRequestUtil;
import net.lab1024.sa.common.apiencrypt.annotation.ApiDecrypt;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.securityprotect.service.SecurityConfigProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-12-09 22:57:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_EMPLOYEE)
public class EmployeeController {

  @Resource private EmployeeService employeeService;

  @Resource private SecurityConfigProvider securityConfigProvider;

  @PostMapping("/employee/query")
  @Operation(summary = "员工管理查询 @author 卓大")
  public ResponseDTO<PageResult<EmployeeVO>> query(@Valid @RequestBody EmployeeQueryForm query) {
    return employeeService.queryEmployee(query);
  }

  @Operation(summary = "添加员工(返回添加员工的密码) @author 卓大")
  @PostMapping("/employee/add")
  @SaCheckPermission("system:employee:add")
  public ResponseDTO<String> addEmployee(@Valid @RequestBody EmployeeAddForm employeeAddForm) {
    return employeeService.addEmployee(employeeAddForm);
  }

  @Operation(summary = "更新员工 @author 卓大")
  @PostMapping("/employee/update")
  @SaCheckPermission("system:employee:update")
  public ResponseDTO<String> updateEmployee(
      @Valid @RequestBody EmployeeUpdateForm employeeUpdateForm) {
    return employeeService.updateEmployee(employeeUpdateForm);
  }

  @Operation(summary = "更新员工个人中心信息 @author 善逸")
  @PostMapping("/employee/update/center")
  public ResponseDTO<String> updateCenter(
      @Valid @RequestBody EmployeeUpdateCenterForm updateCenterForm) {
    updateCenterForm.setEmployeeId(SmartRequestUtil.getRequestUserId());
    return employeeService.updateCenter(updateCenterForm);
  }

  @Operation(summary = "更新登录人头像 @author 善逸")
  @PostMapping("/employee/update/avatar")
  public ResponseDTO<String> updateAvatar(
      @Valid @RequestBody EmployeeUpdateAvatarForm employeeUpdateAvatarForm) {
    employeeUpdateAvatarForm.setEmployeeId(SmartRequestUtil.getRequestUserId());
    return employeeService.updateAvatar(employeeUpdateAvatarForm);
  }

  @Operation(summary = "更新员工禁用/启用状态 @author 卓大")
  @GetMapping("/employee/update/disabled/{employeeId}")
  @SaCheckPermission("system:employee:disabled")
  public ResponseDTO<String> updateDisableFlag(@PathVariable Long employeeId) {
    return employeeService.updateDisableFlag(employeeId);
  }

  @Operation(summary = "批量删除员工 @author 卓大")
  @PostMapping("/employee/update/batch/delete")
  @SaCheckPermission("system:employee:delete")
  public ResponseDTO<String> batchUpdateDeleteFlag(@RequestBody List<Long> employeeIdList) {
    return employeeService.batchUpdateDeleteFlag(employeeIdList);
  }

  @Operation(summary = "批量调整员工部门 @author 卓大")
  @PostMapping("/employee/update/batch/department")
  @SaCheckPermission("system:employee:department:update")
  public ResponseDTO<String> batchUpdateDepartment(
      @Valid @RequestBody EmployeeBatchUpdateDepartmentForm batchUpdateDepartmentForm) {
    return employeeService.batchUpdateDepartment(batchUpdateDepartmentForm);
  }

  @Operation(summary = "修改密码 @author 卓大")
  @PostMapping("/employee/update/password")
  @ApiDecrypt
  public ResponseDTO<String> updatePassword(
      @Valid @RequestBody EmployeeUpdatePasswordForm updatePasswordForm) {
    updatePasswordForm.setEmployeeId(SmartRequestUtil.getRequestUserId());
    return employeeService.updatePassword(SmartRequestUtil.getRequestUser(), updatePasswordForm);
  }

  @Operation(summary = "获取密码复杂度 @author 卓大")
  @GetMapping("/employee/getPasswordComplexityEnabled")
  @ApiDecrypt
  public ResponseDTO<Boolean> getPasswordComplexityEnabled() {
    return ResponseDTO.ok(securityConfigProvider.isPasswordComplexityEnabled());
  }

  @Operation(summary = "重置员工密码 @author 卓大")
  @GetMapping("/employee/update/password/reset/{employeeId}")
  @SaCheckPermission("system:employee:password:reset")
  public ResponseDTO<String> resetPassword(@PathVariable Long employeeId) {
    return employeeService.resetPassword(employeeId);
  }

  @Operation(summary = "查询员工-根据部门id @author 卓大")
  @GetMapping("/employee/getAllEmployeeByDepartmentId/{departmentId}")
  public ResponseDTO<List<EmployeeVO>> getAllEmployeeByDepartmentId(
      @PathVariable Long departmentId) {
    return employeeService.getAllEmployeeByDepartmentId(departmentId);
  }

  @Operation(summary = "查询所有员工 @author 卓大")
  @GetMapping("/employee/queryAll")
  public ResponseDTO<List<EmployeeVO>> queryAllEmployee(
      @RequestParam(value = "disabledFlag", required = false) Boolean disabledFlag) {
    return employeeService.queryAllEmployee(disabledFlag);
  }
}
