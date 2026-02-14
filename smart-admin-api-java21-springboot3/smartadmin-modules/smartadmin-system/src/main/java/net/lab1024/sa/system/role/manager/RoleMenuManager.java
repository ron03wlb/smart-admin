package net.lab1024.sa.system.role.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import net.lab1024.sa.system.role.domain.entity.RoleMenuEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色-菜单 manager
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-09 19:05:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class RoleMenuManager extends ServiceImpl<RoleMenuDao, RoleMenuEntity> {

  private final RoleMenuDao roleMenuDao;

  /** 更新角色权限 */
  @Transactional(rollbackFor = Throwable.class)
  public void updateRoleMenuTransaction(Long roleId, List<RoleMenuEntity> roleMenuEntityList) {
    // 根据角色ID删除菜单权限
    roleMenuDao.deleteByRoleId(roleId);
    // 批量添加菜单权限
    saveBatch(roleMenuEntityList);
  }

  /** 根据角色id集合，查询其所有的菜单权限 */
  public List<MenuVO> getMenuList(List<Long> roleIdList, Boolean administratorFlag) {
    // 管理员返回所有菜单
    if (administratorFlag) {
      List<MenuEntity> menuEntityList =
          roleMenuDao.selectMenuListByRoleIdList(Lists.newArrayList(), false);
      return SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
    }
    // 非管理员 无角色 返回空菜单
    if (CollectionUtils.isEmpty(roleIdList)) {
      return new ArrayList<>();
    }
    List<MenuEntity> menuEntityList = roleMenuDao.selectMenuListByRoleIdList(roleIdList, false);
    return SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
  }
}
