package net.lab1024.sa.admin.module.system.login.manager;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.module.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;
import net.lab1024.sa.base.core.domain.UserPermission;
import net.lab1024.sa.base.module.support.file.service.IFileStorageService;
import net.lab1024.sa.common.cache.constant.CacheKeyConst;
import net.lab1024.sa.common.core.constant.StringConst;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 登录Manager
 *
 * @author 1024创新实验室: 卓大
 * @since 2025-05-03 22:56:34 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class LoginManager {

  @Resource private DepartmentDao departmentDao;

  @Resource private IFileStorageService fileStorageService;

  @Resource private EmployeeDao employeeDao;

  @Resource private RoleEmployeeDao roleEmployeeDao;

  @Resource private RoleMenuDao roleMenuDao;

  /** 获取请求用户信息 */
  @Cached(
      name = CacheKeyConst.Login.REQUEST_EMPLOYEE,
      key = "#requestEmployeeId",
      cacheType = CacheType.BOTH,
      localExpire = 30,
      expire = 120,
      timeUnit = TimeUnit.MINUTES)
  public RequestEmployee getRequestEmployee(Long requestEmployeeId) {
    if (requestEmployeeId == null) {
      return null;
    }
    // 员工基本信息
    EmployeeEntity employeeEntity = employeeDao.selectById(requestEmployeeId);
    if (employeeEntity == null) {
      return null;
    }

    return this.loadLoginInfo(employeeEntity);
  }

  /** 获取登录的用户信息 */
  @CacheUpdate(
      name = CacheKeyConst.Login.REQUEST_EMPLOYEE,
      key = "#employeeEntity.employeeId",
      value = "#result")
  public RequestEmployee loadLoginInfo(EmployeeEntity employeeEntity) {
    // 基础信息
    RequestEmployee requestEmployee = SmartBeanUtil.copy(employeeEntity, RequestEmployee.class);
    requestEmployee.setUserType(UserTypeEnum.ADMIN_EMPLOYEE);

    // 部门信息
    DepartmentVO department = departmentDao.selectDepartmentVO(employeeEntity.getDepartmentId());
    requestEmployee.setDepartmentName(
        null == department ? StringConst.EMPTY : department.getDepartmentName());

    // 头像信息
    String avatar = employeeEntity.getAvatar();
    if (StringUtils.isNotBlank(avatar)) {
      ResponseDTO<String> getFileUrl = fileStorageService.getFileUrl(avatar);
      if (BooleanUtils.isTrue(getFileUrl.getOk())) {
        requestEmployee.setAvatar(getFileUrl.getData());
      }
    }
    return requestEmployee;
  }

  /** 获取用户的权限（包含 角色列表、权限列表） */
  @Cached(
      name = CacheKeyConst.Login.USER_PERMISSION,
      key = "#employeeId",
      cacheType = CacheType.BOTH,
      localExpire = 30,
      expire = 120,
      timeUnit = TimeUnit.MINUTES)
  public UserPermission getUserPermission(Long employeeId) {
    if (null == employeeId) {
      return null;
    }

    return this.loadUserPermission(employeeId);
  }

  /** 获取用户的权限（包含 角色列表、权限列表） */
  @CacheUpdate(name = CacheKeyConst.Login.USER_PERMISSION, key = "#employeeId", value = "#result")
  public UserPermission loadUserPermission(Long employeeId) {
    UserPermission userPermission = new UserPermission();
    userPermission.setPermissionList(new ArrayList<>());
    userPermission.setRoleList(new ArrayList<>());

    // 角色列表
    List<RoleVO> roleList = roleEmployeeDao.selectRoleByEmployeeId(employeeId);
    userPermission
        .getRoleList()
        .addAll(roleList.stream().map(RoleVO::getRoleCode).collect(Collectors.toSet()));

    // 前端菜单和功能点清单
    EmployeeEntity employeeEntity = employeeDao.selectById(employeeId);
    List<Long> roleIdList = roleList.stream().map(RoleVO::getRoleId).collect(Collectors.toList());

    // 直接调用 DAO，避免 Manager 互调
    List<MenuVO> menuAndPointsList;
    if (employeeEntity.getAdministratorFlag()) {
      // 管理员返回所有菜单
      List<MenuEntity> menuEntityList = roleMenuDao.selectMenuListByRoleIdList(List.of(), false);
      menuAndPointsList = SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
    } else if (CollectionUtils.isEmpty(roleIdList)) {
      // 非管理员无角色返回空菜单
      menuAndPointsList = new ArrayList<>();
    } else {
      List<MenuEntity> menuEntityList = roleMenuDao.selectMenuListByRoleIdList(roleIdList, false);
      menuAndPointsList = SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
    }

    // 权限列表
    Set<String> permissionSet = new HashSet<>();
    for (MenuVO menu : menuAndPointsList) {
      if (menu.getPermsType() == null) {
        continue;
      }

      String perms = menu.getApiPerms();
      if (StringUtils.isEmpty(perms)) {
        continue;
      }
      // 接口权限
      String[] split = perms.split(",");
      permissionSet.addAll(Arrays.asList(split));
    }
    userPermission.getPermissionList().addAll(permissionSet);

    return userPermission;
  }

  /** 清除用户权限 */
  @CacheInvalidate(name = CacheKeyConst.Login.USER_PERMISSION, key = "#employeeId")
  public void clearUserPermission(Long employeeId) {}

  /** 清除用户登录信息 */
  @CacheInvalidate(name = CacheKeyConst.Login.REQUEST_EMPLOYEE, key = "#employeeId")
  public void clearUserLoginInfo(Long employeeId) {}
}
