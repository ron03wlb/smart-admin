package net.lab1024.sa.api.system.contract;

import io.vavr.control.Option;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import net.lab1024.sa.api.system.dto.DepartmentDTO;
import net.lab1024.sa.api.system.dto.DepartmentTreeDTO;

/**
 * Department API Contract
 *
 * <p>API contract interface for department-related operations between modules.
 *
 * <p>Design principles: Same as EmployeeContract (Vavr Option, Feign-compatible, high reusability,
 * read-only)
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface DepartmentContract {

  /**
   * Get department and all children IDs (recursive)
   *
   * <p>Returns the department ID itself plus all descendant department IDs in a flat list.
   *
   * @param departmentId department ID (must not be null)
   * @return department ID list (includes self, non-null collection, may be empty if not found)
   * @throws IllegalArgumentException if departmentId is null
   */
  List<Long> getSelfAndChildrenIds(@NotNull Long departmentId);

  /**
   * Get department tree structure
   *
   * <p>Returns the complete department hierarchy as a tree structure.
   *
   * @return department tree list (root departments, non-null collection, may be empty)
   */
  List<DepartmentTreeDTO> getDepartmentTree();

  /**
   * Query all departments (flat list)
   *
   * @return department list (non-null collection, may be empty)
   */
  List<DepartmentDTO> listAll();

  /**
   * Get department by ID
   *
   * @param departmentId department ID (must not be null)
   * @return Option containing department entity (Option.none() if not found)
   * @throws IllegalArgumentException if departmentId is null
   */
  Option<DepartmentDTO> getById(@NotNull Long departmentId);

  /**
   * Get department path (full hierarchical path)
   *
   * <p>Example: "Company / Technology Department / Backend Team"
   *
   * @param departmentId department ID (must not be null)
   * @return Option containing path string (Option.none() if not found)
   * @throws IllegalArgumentException if departmentId is null
   */
  Option<String> getDepartmentPath(@NotNull Long departmentId);
}
