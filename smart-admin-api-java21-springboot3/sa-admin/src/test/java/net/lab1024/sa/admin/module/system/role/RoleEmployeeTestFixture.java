package net.lab1024.sa.admin.module.system.role;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleEmployeeQueryForm;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleEmployeeUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleSelectedVO;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;

/**
 * Test fixtures for RoleEmployee service tests
 *
 * <p>Provides reusable test data builders for RoleEmployee entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create role-employee entity
 * RoleEmployeeEntity entity = RoleEmployeeTestFixture.createEntity(roleId, employeeId);
 *
 * // Create update form
 * RoleEmployeeUpdateForm form = RoleEmployeeTestFixture.createUpdateForm(roleId, employeeIdSet);
 *
 * // Create employee VO
 * EmployeeVO employee = RoleEmployeeTestFixture.createEmployeeVO(employeeId, departmentId);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class RoleEmployeeTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create RoleEmployeeEntity with unique test data
   *
   * @param roleId Role ID
   * @param employeeId Employee ID
   * @return Entity with all required fields set
   */
  public static RoleEmployeeEntity createEntity(Long roleId, Long employeeId) {
    RoleEmployeeEntity entity = new RoleEmployeeEntity(roleId, employeeId);
    entity.setId((long) counter.incrementAndGet());
    entity.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));
    entity.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
    return entity;
  }

  /**
   * Create RoleEmployeeQueryForm with pagination defaults
   *
   * @param roleId Role ID (String type in form)
   * @return Form ready for service.queryEmployee()
   */
  public static RoleEmployeeQueryForm createQueryForm(String roleId) {
    RoleEmployeeQueryForm form = new RoleEmployeeQueryForm();
    form.setRoleId(roleId);
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create RoleEmployeeQueryForm with keywords
   *
   * @param roleId Role ID (String type in form)
   * @param keywords Search keywords
   * @return Form ready for service.queryEmployee()
   */
  public static RoleEmployeeQueryForm createQueryForm(String roleId, String keywords) {
    RoleEmployeeQueryForm form = createQueryForm(roleId);
    form.setKeywords(keywords);
    return form;
  }

  /**
   * Create RoleEmployeeUpdateForm for batch operations
   *
   * @param roleId Role ID
   * @param employeeIds Set of employee IDs
   * @return Form ready for service.batchAddRoleEmployee() or batchRemoveRoleEmployee()
   */
  public static RoleEmployeeUpdateForm createUpdateForm(Long roleId, Set<Long> employeeIds) {
    RoleEmployeeUpdateForm form = new RoleEmployeeUpdateForm();
    form.setRoleId(roleId);
    form.setEmployeeIdList(employeeIds);
    return form;
  }

  /**
   * Create EmployeeVO for query result testing
   *
   * @param employeeId Employee ID
   * @param departmentId Department ID
   * @return VO with all fields set
   */
  public static EmployeeVO createEmployeeVO(Long employeeId, Long departmentId) {
    int id = counter.incrementAndGet();

    EmployeeVO vo = new EmployeeVO();
    vo.setEmployeeId(employeeId);
    vo.setActualName("员工-" + id);
    vo.setLoginName("employee" + id);
    vo.setDepartmentId(departmentId);
    vo.setDisabledFlag(false);

    return vo;
  }

  /**
   * Create RoleSelectedVO for getRoleInfoListByEmployeeId result testing
   *
   * @param roleId Role ID
   * @param roleName Role name
   * @param selected Whether this role is selected
   * @return VO with all fields set
   */
  public static RoleSelectedVO createRoleSelectedVO(
      Long roleId, String roleName, Boolean selected) {
    RoleSelectedVO vo = new RoleSelectedVO();
    vo.setRoleId(roleId);
    vo.setRoleName(roleName);
    vo.setSelected(selected);
    return vo;
  }

  /**
   * Create RoleVO for getRoleIdList result testing
   *
   * @param roleId Role ID
   * @param roleName Role name
   * @return VO with all fields set
   */
  public static RoleVO createRoleVO(Long roleId, String roleName) {
    RoleVO vo = new RoleVO();
    vo.setRoleId(roleId);
    vo.setRoleName(roleName);
    vo.setRoleCode("ROLE_" + roleId);
    vo.setRemark("测试角色-" + roleId);
    return vo;
  }

  /**
   * Create list of RoleEmployeeEntity for batch testing
   *
   * @param roleId Role ID
   * @param employeeIds List of employee IDs
   * @return List of entities
   */
  public static List<RoleEmployeeEntity> createEntityList(Long roleId, List<Long> employeeIds) {
    return employeeIds.stream()
        .map(employeeId -> createEntity(roleId, employeeId))
        .collect(Collectors.toList());
  }

  /**
   * Create list of EmployeeVO for query result testing
   *
   * @param count Number of VOs to create
   * @param departmentId Department ID for all employees
   * @return List of employee VOs
   */
  public static List<EmployeeVO> createEmployeeVOList(int count, Long departmentId) {
    List<EmployeeVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEmployeeVO((long) (100 + i), departmentId));
    }
    return list;
  }

  /**
   * Create list of RoleSelectedVO for getRoleInfoListByEmployeeId result testing
   *
   * @param count Number of VOs to create
   * @param selectedRoleIds Set of selected role IDs
   * @return List of role selected VOs
   */
  public static List<RoleSelectedVO> createRoleSelectedVOList(
      int count, Set<Long> selectedRoleIds) {
    List<RoleSelectedVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Long roleId = (long) (i + 1);
      boolean selected = selectedRoleIds.contains(roleId);
      list.add(createRoleSelectedVO(roleId, "角色-" + (i + 1), selected));
    }
    return list;
  }

  /**
   * Create list of RoleVO for getRoleIdList result testing
   *
   * @param count Number of VOs to create
   * @return List of role VOs
   */
  public static List<RoleVO> createRoleVOList(int count) {
    List<RoleVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createRoleVO((long) (i + 1), "角色-" + (i + 1)));
    }
    return list;
  }

  /**
   * Create Set of employee IDs for batch operations
   *
   * @param ids Employee IDs
   * @return Set of employee IDs
   */
  public static Set<Long> createEmployeeIdSet(Long... ids) {
    Set<Long> set = new HashSet<>();
    for (Long id : ids) {
      set.add(id);
    }
    return set;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
