package net.lab1024.sa.system.adapter;

import io.vavr.control.Option;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.system.contract.RoleContract;
import net.lab1024.sa.api.system.dto.RoleDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.role.dao.RoleDao;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * Role Contract Adapter
 *
 * <p>Adapts RoleService to RoleContract API.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class RoleContractAdapter implements RoleContract {

  private final RoleDao roleDao;

  @Override
  public Option<RoleDTO> getById(Long roleId) {
    if (roleId == null) {
      throw new IllegalArgumentException("roleId cannot be null");
    }

    return Option.of(roleDao.selectById(roleId))
        .map(entity -> SmartBeanUtil.copy(entity, RoleDTO.class));
  }

  @Override
  public List<RoleDTO> queryByIds(Collection<Long> roleIds) {
    if (roleIds == null) {
      throw new IllegalArgumentException("roleIds cannot be null");
    }

    if (CollectionUtils.isEmpty(roleIds)) {
      return Collections.emptyList();
    }

    return roleDao.selectBatchIds(roleIds).stream()
        .map(entity -> SmartBeanUtil.copy(entity, RoleDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public List<RoleDTO> listAll() {
    return roleDao.selectList(null).stream()
        .map(entity -> SmartBeanUtil.copy(entity, RoleDTO.class))
        .collect(Collectors.toList());
  }
}
