package net.lab1024.sa.api.system.contract;

import io.vavr.control.Option;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import net.lab1024.sa.api.system.dto.EmployeeDTO;

/**
 * Employee API Contract
 *
 * <p>API contract interface for employee-related operations between modules.
 *
 * <p>Design principles:
 *
 * <ul>
 *   <li>Use Vavr Option for type safety (NEVER return null directly)
 *   <li>Adapt to Feign remote calls (future microservices architecture)
 *   <li>High reusability methods only
 *   <li>Read-only query operations (no side effects)
 * </ul>
 *
 * <p>Current usage: Internal method calls within monolithic architecture (0.1ms latency)
 *
 * <p>Future usage: Feign/RPC remote calls in microservices architecture (requires @FeignClient
 * annotation only)
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface EmployeeContract {

  /**
   * Query all employees by department ID
   *
   * @param departmentId department ID (must not be null)
   * @return employee list (non-null collection, may be empty)
   * @throws IllegalArgumentException if departmentId is null
   */
  List<EmployeeDTO> queryByDepartmentId(@NotNull Long departmentId);

  /**
   * Query all employees by disabled status
   *
   * @param disabledFlag disabled status (null means all employees)
   * @return employee list (non-null collection, may be empty)
   */
  List<EmployeeDTO> queryAll(Boolean disabledFlag);

  /**
   * Get employee by login name
   *
   * @param loginName login name (must not be null)
   * @return Option containing employee entity (Option.none() if not found)
   * @throws IllegalArgumentException if loginName is null or blank
   */
  Option<EmployeeDTO> getByLoginName(@NotNull String loginName);

  /**
   * Batch query employees by IDs
   *
   * @param employeeIds employee ID collection (must not be null)
   * @return employee list (ordered by input IDs, non-null collection, may be empty)
   * @throws IllegalArgumentException if employeeIds is null
   */
  List<EmployeeDTO> queryByIds(@NotNull Collection<Long> employeeIds);

  /**
   * Get employee by ID
   *
   * @param employeeId employee ID (must not be null)
   * @return Option containing employee entity (Option.none() if not found)
   * @throws IllegalArgumentException if employeeId is null
   */
  Option<EmployeeDTO> getById(@NotNull Long employeeId);
}
