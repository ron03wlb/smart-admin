package net.lab1024.sa.system.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.system.contract.DepartmentContract;
import net.lab1024.sa.api.system.dto.DepartmentDTO;
import net.lab1024.sa.api.system.dto.DepartmentTreeDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.system.department.service.DepartmentService;
import org.springframework.stereotype.Component;

/**
 * Department Contract Adapter
 *
 * <p>Adapts DepartmentService to DepartmentContract API.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class DepartmentContractAdapter implements DepartmentContract {

  private final DepartmentService departmentService;
  private final DepartmentCacheManager departmentCacheManager;
  private final DepartmentDao departmentDao;

  @Override
  public List<Long> getSelfAndChildrenIds(Long departmentId) {
    if (departmentId == null) {
      throw new IllegalArgumentException("departmentId cannot be null");
    }

    return departmentService.selfAndChildrenIdList(departmentId);
  }

  @Override
  public List<DepartmentTreeDTO> getDepartmentTree() {
    return departmentCacheManager.getDepartmentTree().stream()
        .map(treeVO -> SmartBeanUtil.copy(treeVO, DepartmentTreeDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public List<DepartmentDTO> listAll() {
    return departmentService.listAll().stream()
        .map(vo -> SmartBeanUtil.copy(vo, DepartmentDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public Option<DepartmentDTO> getById(Long departmentId) {
    if (departmentId == null) {
      throw new IllegalArgumentException("departmentId cannot be null");
    }

    return Option.of(departmentDao.selectById(departmentId))
        .map(entity -> SmartBeanUtil.copy(entity, DepartmentDTO.class));
  }

  @Override
  public Option<String> getDepartmentPath(Long departmentId) {
    if (departmentId == null) {
      throw new IllegalArgumentException("departmentId cannot be null");
    }

    return Option.of(departmentService.getDepartmentPath(departmentId));
  }
}
