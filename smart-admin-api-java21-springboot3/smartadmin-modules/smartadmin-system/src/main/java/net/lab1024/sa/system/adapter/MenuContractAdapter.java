package net.lab1024.sa.system.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.system.contract.MenuContract;
import net.lab1024.sa.api.system.dto.MenuDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.menu.dao.MenuDao;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import org.springframework.stereotype.Component;

/**
 * Menu Contract Adapter
 *
 * <p>Adapts MenuService to MenuContract API.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class MenuContractAdapter implements MenuContract {

  private final MenuDao menuDao;
  private final RoleMenuDao roleMenuDao;

  @Override
  public Option<MenuDTO> getById(Long menuId) {
    if (menuId == null) {
      throw new IllegalArgumentException("menuId cannot be null");
    }

    return Option.of(menuDao.selectById(menuId))
        .map(entity -> SmartBeanUtil.copy(entity, MenuDTO.class));
  }

  @Override
  public List<MenuDTO> listAll() {
    return menuDao.selectList(null).stream()
        .map(entity -> SmartBeanUtil.copy(entity, MenuDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public List<MenuDTO> queryByRoleId(Long roleId) {
    if (roleId == null) {
      throw new IllegalArgumentException("roleId cannot be null");
    }

    return roleMenuDao.selectMenuListByRoleIdList(List.of(roleId), Boolean.FALSE).stream()
        .map(entity -> SmartBeanUtil.copy(entity, MenuDTO.class))
        .collect(Collectors.toList());
  }
}
