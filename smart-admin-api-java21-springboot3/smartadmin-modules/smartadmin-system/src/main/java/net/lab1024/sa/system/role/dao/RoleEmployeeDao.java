package net.lab1024.sa.system.role.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Set;
import net.lab1024.sa.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.system.role.domain.entity.RoleEmployeeEntity;
import net.lab1024.sa.system.role.domain.form.RoleEmployeeQueryForm;
import net.lab1024.sa.system.role.domain.vo.RoleEmployeeVO;
import net.lab1024.sa.system.role.domain.vo.RoleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色 员工 dao
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-03-07 18:54:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface RoleEmployeeDao extends BaseMapper<RoleEmployeeEntity> {

  String EMPLOYEE_ID = "employeeId";
  String ROLE_ID = "roleId";

  /** 根据员工id 查询所有的角色 */
  List<RoleVO> selectRoleByEmployeeId(@Param(EMPLOYEE_ID) Long employeeId);

  /** 根据员工id 查询所有的角色io集合 */
  List<Long> selectRoleIdByEmployeeId(@Param(EMPLOYEE_ID) Long employeeId);

  /** 根据员工id 查询所有的角色 */
  List<RoleEmployeeEntity> selectRoleIdByEmployeeIdList(
      @Param("employeeIdList") List<Long> employeeIdList);

  /** 根据员工id 查询所有的角色 */
  List<RoleEmployeeVO> selectRoleByEmployeeIdList(
      @Param("employeeIdList") List<Long> employeeIdList);

  /** 查询角色下的人员id */
  Set<Long> selectEmployeeIdByRoleIdList(@Param("roleIdList") List<Long> roleIdList);

  /** */
  List<EmployeeVO> selectRoleEmployeeByName(
      Page page, @Param("queryForm") RoleEmployeeQueryForm roleEmployeeQueryForm);

  /** */
  List<EmployeeVO> selectEmployeeByRoleId(@Param(ROLE_ID) Long roleId);

  /** 根据员工信息删除 */
  void deleteByEmployeeId(@Param(EMPLOYEE_ID) Long employeeId);

  /** 删除某个角色的所有关系 */
  void deleteByRoleId(@Param(ROLE_ID) Long roleId);

  /** 根据员工和 角色删除关系 */
  void deleteByEmployeeIdRoleId(@Param(EMPLOYEE_ID) Long employeeId, @Param(ROLE_ID) Long roleId);

  /** 批量删除某个角色下的某批用户的关联关系 */
  void batchDeleteEmployeeRole(
      @Param(ROLE_ID) Long roleId, @Param("employeeIds") Set<Long> employeeIds);

  /** 判断某个角色下是否存在用户 */
  Integer existsByRoleId(@Param(ROLE_ID) Long roleId);
}
