package net.lab1024.sa.system.adapter;

import io.vavr.control.Option;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.system.contract.EmployeeContract;
import net.lab1024.sa.api.system.dto.EmployeeDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.employee.service.EmployeeService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * Employee Contract Adapter
 *
 * <p>Adapts EmployeeService to EmployeeContract API.
 *
 * <p>Design pattern: Adapter pattern - Converts Service layer to API contract layer with proper
 * type safety (Vavr Option) and DTO transformation.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class EmployeeContractAdapter implements EmployeeContract {

  private final EmployeeService employeeService;
  private final EmployeeDao employeeDao;

  @Override
  public List<EmployeeDTO> queryByDepartmentId(Long departmentId) {
    if (departmentId == null) {
      throw new IllegalArgumentException("departmentId cannot be null");
    }

    return Option.of(employeeService.getAllEmployeeByDepartmentId(departmentId))
        .map(response -> response.getData())
        .map(
            voList ->
                voList.stream()
                    .map(vo -> SmartBeanUtil.copy(vo, EmployeeDTO.class))
                    .collect(Collectors.toList()))
        .getOrElse(Collections.emptyList());
  }

  @Override
  public List<EmployeeDTO> queryAll(Boolean disabledFlag) {
    return Option.of(employeeService.queryAllEmployee(disabledFlag))
        .map(response -> response.getData())
        .map(
            voList ->
                voList.stream()
                    .map(vo -> SmartBeanUtil.copy(vo, EmployeeDTO.class))
                    .collect(Collectors.toList()))
        .getOrElse(Collections.emptyList());
  }

  @Override
  public Option<EmployeeDTO> getByLoginName(String loginName) {
    if (loginName == null || loginName.isBlank()) {
      throw new IllegalArgumentException("loginName cannot be null or blank");
    }

    return Option.of(employeeService.getByLoginName(loginName))
        .map(entity -> SmartBeanUtil.copy(entity, EmployeeDTO.class));
  }

  @Override
  public List<EmployeeDTO> queryByIds(Collection<Long> employeeIds) {
    if (employeeIds == null) {
      throw new IllegalArgumentException("employeeIds cannot be null");
    }

    if (CollectionUtils.isEmpty(employeeIds)) {
      return Collections.emptyList();
    }

    List<EmployeeEntity> entities = employeeDao.selectBatchIds(employeeIds);
    return entities.stream()
        .map(entity -> SmartBeanUtil.copy(entity, EmployeeDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public Option<EmployeeDTO> getById(Long employeeId) {
    if (employeeId == null) {
      throw new IllegalArgumentException("employeeId cannot be null");
    }

    return Option.of(employeeService.getById(employeeId))
        .map(entity -> SmartBeanUtil.copy(entity, EmployeeDTO.class));
  }
}
