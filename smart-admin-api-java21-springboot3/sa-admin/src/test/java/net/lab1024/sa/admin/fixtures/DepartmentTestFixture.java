package net.lab1024.sa.admin.fixtures;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;

/**
 * Test fixture for Department domain objects
 *
 * <p>Provides factory methods to create test data for Department-related tests, including tree
 * structures for testing recursive algorithms.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Create single department
 * DepartmentEntity dept = DepartmentTestFixture.createDepartment();
 *
 * // Create department tree structure
 * List<DepartmentEntity> tree = DepartmentTestFixture.createDepartmentTree();
 * // Tree structure:
 * // - Company (ID: 1, parentId: 0)
 * //   - R&D Department (ID: 2, parentId: 1)
 * //     - Backend Team (ID: 4, parentId: 2)
 * //     - Frontend Team (ID: 5, parentId: 2)
 * //   - Marketing Department (ID: 3, parentId: 1)
 * //     - Promotion Team (ID: 6, parentId: 3)
 * }</pre>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
public class DepartmentTestFixture {

  private static final Long DEFAULT_DEPARTMENT_ID = 1L;
  private static final String DEFAULT_DEPARTMENT_NAME = "Test Department";
  private static final Long ROOT_PARENT_ID = 0L;

  /**
   * Create department entity with default values
   *
   * @return DepartmentEntity with default test data
   */
  public static DepartmentEntity createDepartment() {
    return createDepartment(DEFAULT_DEPARTMENT_ID, DEFAULT_DEPARTMENT_NAME);
  }

  /**
   * Create department entity with specific ID and name
   *
   * @param departmentId Department ID
   * @param departmentName Department name
   * @return DepartmentEntity with specified values
   */
  public static DepartmentEntity createDepartment(Long departmentId, String departmentName) {
    return createDepartment(departmentId, ROOT_PARENT_ID, departmentName, departmentId.intValue());
  }

  /**
   * Create department entity with full customization
   *
   * @param departmentId Department ID
   * @param parentId Parent department ID (0 for root)
   * @param departmentName Department name
   * @param sort Sort order
   * @return DepartmentEntity with specified values
   */
  public static DepartmentEntity createDepartment(
      Long departmentId, Long parentId, String departmentName, Integer sort) {
    DepartmentEntity entity = new DepartmentEntity();
    entity.setDepartmentId(departmentId);
    entity.setDepartmentName(departmentName);
    entity.setParentId(parentId);
    entity.setManagerId(null);
    entity.setSort(sort);
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    return entity;
  }

  /**
   * Create a department tree structure for testing recursive algorithms
   *
   * <p>Tree structure:
   *
   * <pre>
   * Company (ID: 1, parentId: 0)
   *   ├── R&D Department (ID: 2, parentId: 1)
   *   │   ├── Backend Team (ID: 4, parentId: 2)
   *   │   └── Frontend Team (ID: 5, parentId: 2)
   *   └── Marketing Department (ID: 3, parentId: 1)
   *       └── Promotion Team (ID: 6, parentId: 3)
   * </pre>
   *
   * @return List of departments forming a tree structure
   */
  public static List<DepartmentEntity> createDepartmentTree() {
    List<DepartmentEntity> departments = new ArrayList<>();

    // Root
    departments.add(createDepartment(1L, 0L, "Company", 1));

    // Level 1 children
    departments.add(createDepartment(2L, 1L, "R&D Department", 2));
    departments.add(createDepartment(3L, 1L, "Marketing Department", 3));

    // Level 2 children
    departments.add(createDepartment(4L, 2L, "Backend Team", 4));
    departments.add(createDepartment(5L, 2L, "Frontend Team", 5));
    departments.add(createDepartment(6L, 3L, "Promotion Team", 6));

    return departments;
  }

  /**
   * Create a deep department tree (5+ levels) for testing deep recursion
   *
   * @return List of departments forming a deep tree
   */
  public static List<DepartmentEntity> createDeepDepartmentTree() {
    List<DepartmentEntity> departments = new ArrayList<>();

    // 5-level deep tree
    departments.add(createDepartment(1L, 0L, "Level 1 Root", 1));
    departments.add(createDepartment(2L, 1L, "Level 2 Child", 2));
    departments.add(createDepartment(3L, 2L, "Level 3 Child", 3));
    departments.add(createDepartment(4L, 3L, "Level 4 Child", 4));
    departments.add(createDepartment(5L, 4L, "Level 5 Leaf", 5));

    return departments;
  }

  /**
   * Create multiple root departments (for multi-tree testing)
   *
   * @return List of departments with multiple root nodes
   */
  public static List<DepartmentEntity> createMultiRootTree() {
    List<DepartmentEntity> departments = new ArrayList<>();

    // Root 1 and its children
    departments.add(createDepartment(1L, 0L, "Company A", 1));
    departments.add(createDepartment(2L, 1L, "Dept A1", 2));

    // Root 2 and its children
    departments.add(createDepartment(10L, 0L, "Company B", 10));
    departments.add(createDepartment(11L, 10L, "Dept B1", 11));

    return departments;
  }

  /**
   * Create single leaf department (no children)
   *
   * @return DepartmentEntity without children
   */
  public static DepartmentEntity createLeafDepartment() {
    return createDepartment(100L, 1L, "Leaf Department", 100);
  }

  /**
   * Create department with specific manager
   *
   * @param managerId Manager employee ID
   * @return DepartmentEntity with manager assigned
   */
  public static DepartmentEntity createDepartmentWithManager(Long managerId) {
    DepartmentEntity dept = createDepartment();
    dept.setManagerId(managerId);
    return dept;
  }
}
