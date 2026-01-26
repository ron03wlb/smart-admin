package net.lab1024.sa.admin.module.system.role.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import net.lab1024.sa.admin.module.system.role.dao.RoleDataScopeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色 数据范围 manager
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class RoleDataScopeManager extends ServiceImpl<RoleDataScopeDao, RoleDataScopeEntity> {

  /** 批量更新角色数据范围（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void updateRoleDataScopeListTransaction(
      Long roleId, List<RoleDataScopeEntity> roleDataScopeEntityList) {
    // 删除旧的数据范围
    this.getBaseMapper().deleteByRoleId(roleId);
    // 批量保存新的数据范围
    this.saveBatch(roleDataScopeEntityList);
  }
}
