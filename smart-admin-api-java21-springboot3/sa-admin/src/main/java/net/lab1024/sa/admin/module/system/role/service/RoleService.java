package net.lab1024.sa.admin.module.system.role.service;

import jakarta.annotation.Resource;
import java.util.List;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleAddForm;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;
import net.lab1024.sa.admin.module.system.role.manager.RoleManager;
import net.lab1024.sa.common.core.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 角色
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-08-16 20:19:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class RoleService {

  @Resource private RoleDao roleDao;

  @Resource private RoleEmployeeDao roleEmployeeDao;

  @Resource private RoleManager roleManager;

  /** 新增添加角色 */
  public ResponseDTO<String> addRole(RoleAddForm roleAddForm) {
    RoleEntity existRoleEntity = roleDao.getByRoleName(roleAddForm.getRoleName());
    if (null != existRoleEntity) {
      return ResponseDTO.userErrorParam("角色名称重复");
    }

    existRoleEntity = roleDao.getByRoleCode(roleAddForm.getRoleCode());
    if (null != existRoleEntity) {
      return ResponseDTO.userErrorParam("角色编码重复，重复的角色为：" + existRoleEntity.getRoleName());
    }

    RoleEntity roleEntity = SmartBeanUtil.copy(roleAddForm, RoleEntity.class);
    roleDao.insert(roleEntity);
    return ResponseDTO.ok();
  }

  /** 根据角色id 删除 */
  public ResponseDTO<String> deleteRole(Long roleId) {
    RoleEntity roleEntity = roleDao.selectById(roleId);
    if (null == roleEntity) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }
    // 当没有员工绑定这个角色时才可以删除
    Integer exists = roleEmployeeDao.existsByRoleId(roleId);
    if (exists != null) {
      return ResponseDTO.error(UserErrorCode.ALREADY_EXIST, "该角色下存在员工，无法删除");
    }
    // 委托给 Manager 层处理事务性删除操作
    roleManager.deleteRoleWithCascade(roleId);
    return ResponseDTO.ok();
  }

  /** 更新角色 */
  public ResponseDTO<String> updateRole(RoleUpdateForm roleUpdateForm) {
    if (null == roleDao.selectById(roleUpdateForm.getRoleId())) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }

    RoleEntity existRoleEntity = roleDao.getByRoleName(roleUpdateForm.getRoleName());
    if (null != existRoleEntity
        && !existRoleEntity.getRoleId().equals(roleUpdateForm.getRoleId())) {
      return ResponseDTO.userErrorParam("角色名称重复");
    }

    existRoleEntity = roleDao.getByRoleCode(roleUpdateForm.getRoleCode());
    if (null != existRoleEntity
        && !existRoleEntity.getRoleId().equals(roleUpdateForm.getRoleId())) {
      return ResponseDTO.userErrorParam("角色编码重复，重复的角色为：" + existRoleEntity.getRoleName());
    }

    RoleEntity roleEntity = SmartBeanUtil.copy(roleUpdateForm, RoleEntity.class);
    // 委托给 Manager 层处理事务性更新操作
    roleManager.updateRole(roleEntity);
    return ResponseDTO.ok();
  }

  /** 根据id获取角色数据 */
  public ResponseDTO<RoleVO> getRoleById(Long roleId) {
    RoleEntity roleEntity = roleDao.selectById(roleId);
    if (null == roleEntity) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }
    RoleVO role = SmartBeanUtil.copy(roleEntity, RoleVO.class);
    return ResponseDTO.ok(role);
  }

  /** 获取所有角色列表 */
  public ResponseDTO<List<RoleVO>> getAllRole() {
    List<RoleEntity> roleEntityList = roleDao.selectList(null);
    List<RoleVO> roleList = SmartBeanUtil.copyList(roleEntityList, RoleVO.class);
    return ResponseDTO.ok(roleList);
  }
}
