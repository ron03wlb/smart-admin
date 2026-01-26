package net.lab1024.sa.admin.module.system.role.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色 Manager
 *
 * <p>负责角色相关的事务性操作和缓存管理
 *
 * @author 1024创新实验室
 * @since 2025-01-22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class RoleManager extends ServiceImpl<RoleDao, RoleEntity> {

  private final RoleDao roleDao;

  private final RoleMenuDao roleMenuDao;

  private final RoleEmployeeDao roleEmployeeDao;

  /**
   * 删除角色（级联删除关联数据）
   *
   * @param roleId 角色ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public void deleteRoleWithCascadeTransaction(Long roleId) {
    // 删除角色
    roleDao.deleteById(roleId);
    // 删除角色菜单关联
    roleMenuDao.deleteByRoleId(roleId);
    // 删除角色员工关联
    roleEmployeeDao.deleteByRoleId(roleId);
  }

  /**
   * 更新角色信息
   *
   * @param roleEntity 角色实体
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateRoleTransaction(RoleEntity roleEntity) {
    roleDao.updateById(roleEntity);
  }
}
