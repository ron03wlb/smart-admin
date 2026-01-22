package net.lab1024.sa.admin.module.system.role.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleAddForm;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;
import net.lab1024.sa.admin.module.system.role.manager.RoleManager;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.code.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * RoleService Unit Tests
 *
 * <p>Tests the Service layer's business logic for role management including validation, uniqueness
 * checks, and delegation to Manager layer for transactional operations.
 *
 * <p><b>Testing Strategy:</b>
 *
 * <ul>
 *   <li>Pure unit tests with Mockito (no Spring context)
 *   <li>Mock all DAO and Manager dependencies
 *   <li>Test business logic: uniqueness validation, existence checks, error handling
 *   <li>Verify delegation to Manager layer for @Transactional operations
 * </ul>
 *
 * <p><b>Methods Tested:</b>
 *
 * <ol>
 *   <li>addRole() - Validates uniqueness of roleName and roleCode, then inserts
 *   <li>deleteRole() - Checks existence and employee assignment, delegates to Manager
 *   <li>updateRole() - Validates existence and uniqueness (excluding same ID), delegates to Manager
 *   <li>getRoleById() - Retrieves role by ID with error handling
 *   <li>getAllRole() - Retrieves all roles
 * </ol>
 *
 * @author Claude Code
 * @since 2026-01-22
 */
@DisplayName("RoleService Unit Tests - Business Logic")
class RoleServiceTest extends BaseUnitTest {

  @InjectMocks private RoleService roleService;

  @Mock private RoleDao roleDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleManager roleManager;

  private RoleAddForm testAddForm;
  private RoleUpdateForm testUpdateForm;
  private RoleEntity testRoleEntity;
  private RoleEntity existingRoleEntity;

  private static final Long TEST_ROLE_ID = 1L;
  private static final String TEST_ROLE_NAME = "Test Role";
  private static final String TEST_ROLE_CODE = "TEST_ROLE";
  private static final String TEST_REMARK = "Test role description";

  @BeforeEach
  void setUp() {
    // Create test add form
    testAddForm = new RoleAddForm();
    testAddForm.setRoleName(TEST_ROLE_NAME);
    testAddForm.setRoleCode(TEST_ROLE_CODE);
    testAddForm.setRemark(TEST_REMARK);

    // Create test update form
    testUpdateForm = new RoleUpdateForm();
    testUpdateForm.setRoleId(TEST_ROLE_ID);
    testUpdateForm.setRoleName(TEST_ROLE_NAME);
    testUpdateForm.setRoleCode(TEST_ROLE_CODE);
    testUpdateForm.setRemark(TEST_REMARK);

    // Create test role entity
    testRoleEntity = new RoleEntity();
    testRoleEntity.setRoleId(TEST_ROLE_ID);
    testRoleEntity.setRoleName(TEST_ROLE_NAME);
    testRoleEntity.setRoleCode(TEST_ROLE_CODE);
    testRoleEntity.setRemark(TEST_REMARK);

    // Create existing role entity (for conflict scenarios)
    existingRoleEntity = new RoleEntity();
    existingRoleEntity.setRoleId(999L);
    existingRoleEntity.setRoleName("Existing Role");
    existingRoleEntity.setRoleCode("EXISTING_ROLE");
  }

  @Nested
  @DisplayName("addRole() Tests - Uniqueness Validation + Insert")
  class AddRoleTests {

    @Test
    @DisplayName("Should successfully create role when no conflicts exist")
    void addRole_ValidData_CreatesRole() {
      // Given
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(null);
      when(roleDao.insert(any(RoleEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = roleService.addRole(testAddForm);

      // Then
      assertOk(result);
      verify(roleDao, times(1)).getByRoleName(TEST_ROLE_NAME);
      verify(roleDao, times(1)).getByRoleCode(TEST_ROLE_CODE);
      verify(roleDao, times(1)).insert(any(RoleEntity.class));
    }

    @Test
    @DisplayName("Should reject when role name already exists")
    void addRole_DuplicateRoleName_ReturnsError() {
      // Given
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(existingRoleEntity);

      // When
      ResponseDTO<String> result = roleService.addRole(testAddForm);

      // Then
      assertErrorContains(result, "角色名称重复");
      verify(roleDao, times(1)).getByRoleName(TEST_ROLE_NAME);
      verify(roleDao, never()).getByRoleCode(anyString());
      verify(roleDao, never()).insert(any(RoleEntity.class));
    }

    @Test
    @DisplayName("Should reject when role code already exists")
    void addRole_DuplicateRoleCode_ReturnsError() {
      // Given
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(existingRoleEntity);

      // When
      ResponseDTO<String> result = roleService.addRole(testAddForm);

      // Then
      assertErrorContains(result, "角色编码重复");
      assertErrorContains(result, existingRoleEntity.getRoleName());
      verify(roleDao, times(1)).getByRoleName(TEST_ROLE_NAME);
      verify(roleDao, times(1)).getByRoleCode(TEST_ROLE_CODE);
      verify(roleDao, never()).insert(any(RoleEntity.class));
    }

    @Test
    @DisplayName("Should check role name uniqueness before role code")
    void addRole_ChecksRoleNameFirst() {
      // Given
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(existingRoleEntity);

      // When
      ResponseDTO<String> result = roleService.addRole(testAddForm);

      // Then
      assertErrorContains(result, "角色名称重复");
      verify(roleDao, times(1)).getByRoleName(TEST_ROLE_NAME);
      verify(roleDao, never()).getByRoleCode(anyString()); // Never reaches code check
    }

    @Test
    @DisplayName("Should convert form to entity before inserting")
    void addRole_ConvertsFormToEntity() {
      // Given
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(null);
      when(roleDao.insert(any(RoleEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = roleService.addRole(testAddForm);

      // Then
      assertOk(result);
      verify(roleDao, times(1)).insert(any(RoleEntity.class));
    }
  }

  @Nested
  @DisplayName("deleteRole() Tests - Existence + Employee Check + Delegation")
  class DeleteRoleTests {

    @Test
    @DisplayName("Should successfully delete role when no employees assigned")
    void deleteRole_NoEmployees_DeletesRole() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleEmployeeDao.existsByRoleId(TEST_ROLE_ID)).thenReturn(null); // No employees
      doNothing().when(roleManager).deleteRoleWithCascade(TEST_ROLE_ID);

      // When
      ResponseDTO<String> result = roleService.deleteRole(TEST_ROLE_ID);

      // Then
      assertOk(result);
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
      verify(roleEmployeeDao, times(1)).existsByRoleId(TEST_ROLE_ID);
      verify(roleManager, times(1)).deleteRoleWithCascade(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should reject when role does not exist")
    void deleteRole_NonExistentRole_ReturnsError() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.deleteRole(TEST_ROLE_ID);

      // Then
      assertError(result, UserErrorCode.DATA_NOT_EXIST);
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
      verify(roleEmployeeDao, never()).existsByRoleId(any());
      verify(roleManager, never()).deleteRoleWithCascade(any());
    }

    @Test
    @DisplayName("Should reject when employees are assigned to role")
    void deleteRole_HasEmployees_ReturnsError() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleEmployeeDao.existsByRoleId(TEST_ROLE_ID)).thenReturn(1); // Has employees

      // When
      ResponseDTO<String> result = roleService.deleteRole(TEST_ROLE_ID);

      // Then
      assertError(result, UserErrorCode.ALREADY_EXIST);
      assertErrorContains(result, "该角色下存在员工");
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
      verify(roleEmployeeDao, times(1)).existsByRoleId(TEST_ROLE_ID);
      verify(roleManager, never()).deleteRoleWithCascade(any());
    }

    @Test
    @DisplayName("Should delegate to Manager for transactional delete")
    void deleteRole_DelegatesToManager() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleEmployeeDao.existsByRoleId(TEST_ROLE_ID)).thenReturn(null);
      doNothing().when(roleManager).deleteRoleWithCascade(TEST_ROLE_ID);

      // When
      ResponseDTO<String> result = roleService.deleteRole(TEST_ROLE_ID);

      // Then
      assertOk(result);
      verify(roleManager, times(1)).deleteRoleWithCascade(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should check existence before checking employee assignment")
    void deleteRole_ChecksExistenceFirst() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.deleteRole(TEST_ROLE_ID);

      // Then
      assertError(result, UserErrorCode.DATA_NOT_EXIST);
      verify(roleEmployeeDao, never()).existsByRoleId(any()); // Never checks employees
    }
  }

  @Nested
  @DisplayName("updateRole() Tests - Existence + Uniqueness (Exclude Same ID) + Delegation")
  class UpdateRoleTests {

    @Test
    @DisplayName("Should successfully update role when no conflicts exist")
    void updateRole_ValidData_UpdatesRole() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(null);
      doNothing().when(roleManager).updateRole(any(RoleEntity.class));

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertOk(result);
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
      verify(roleDao, times(1)).getByRoleName(TEST_ROLE_NAME);
      verify(roleDao, times(1)).getByRoleCode(TEST_ROLE_CODE);
      verify(roleManager, times(1)).updateRole(any(RoleEntity.class));
    }

    @Test
    @DisplayName("Should reject when role does not exist")
    void updateRole_NonExistentRole_ReturnsError() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertError(result, UserErrorCode.DATA_NOT_EXIST);
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
      verify(roleDao, never()).getByRoleName(anyString());
      verify(roleDao, never()).getByRoleCode(anyString());
      verify(roleManager, never()).updateRole(any());
    }

    @Test
    @DisplayName("Should reject when role name conflicts with different role")
    void updateRole_DuplicateRoleName_ReturnsError() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(existingRoleEntity); // Different ID

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertErrorContains(result, "角色名称重复");
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
      verify(roleDao, times(1)).getByRoleName(TEST_ROLE_NAME);
      verify(roleDao, never()).getByRoleCode(anyString());
      verify(roleManager, never()).updateRole(any());
    }

    @Test
    @DisplayName("Should reject when role code conflicts with different role")
    void updateRole_DuplicateRoleCode_ReturnsError() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(existingRoleEntity); // Different ID

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertErrorContains(result, "角色编码重复");
      assertErrorContains(result, existingRoleEntity.getRoleName());
      verify(roleDao, times(1)).getByRoleCode(TEST_ROLE_CODE);
      verify(roleManager, never()).updateRole(any());
    }

    @Test
    @DisplayName("Should allow update when role name belongs to same role ID")
    void updateRole_SameRoleIdName_Allowed() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(testRoleEntity); // Same ID
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(null);
      doNothing().when(roleManager).updateRole(any(RoleEntity.class));

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertOk(result); // Should succeed - same role
      verify(roleManager, times(1)).updateRole(any(RoleEntity.class));
    }

    @Test
    @DisplayName("Should allow update when role code belongs to same role ID")
    void updateRole_SameRoleIdCode_Allowed() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(testRoleEntity); // Same ID
      doNothing().when(roleManager).updateRole(any(RoleEntity.class));

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertOk(result); // Should succeed - same role
      verify(roleManager, times(1)).updateRole(any(RoleEntity.class));
    }

    @Test
    @DisplayName("Should delegate to Manager for transactional update")
    void updateRole_DelegatesToManager() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);
      when(roleDao.getByRoleName(TEST_ROLE_NAME)).thenReturn(null);
      when(roleDao.getByRoleCode(TEST_ROLE_CODE)).thenReturn(null);
      doNothing().when(roleManager).updateRole(any(RoleEntity.class));

      // When
      ResponseDTO<String> result = roleService.updateRole(testUpdateForm);

      // Then
      assertOk(result);
      verify(roleManager, times(1)).updateRole(any(RoleEntity.class));
    }
  }

  @Nested
  @DisplayName("getRoleById() Tests - Simple Retrieval")
  class GetRoleByIdTests {

    @Test
    @DisplayName("Should return role when found")
    void getRoleById_ExistingRole_ReturnsRole() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);

      // When
      ResponseDTO<RoleVO> result = roleService.getRoleById(TEST_ROLE_ID);

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertEquals(TEST_ROLE_ID, result.getData().getRoleId());
      assertEquals(TEST_ROLE_NAME, result.getData().getRoleName());
      assertEquals(TEST_ROLE_CODE, result.getData().getRoleCode());
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should return error when role not found")
    void getRoleById_NonExistentRole_ReturnsError() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(null);

      // When
      ResponseDTO<RoleVO> result = roleService.getRoleById(TEST_ROLE_ID);

      // Then
      assertError(result, UserErrorCode.DATA_NOT_EXIST);
      assertNull(result.getData());
      verify(roleDao, times(1)).selectById(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should convert entity to VO")
    void getRoleById_ConvertsEntityToVO() {
      // Given
      when(roleDao.selectById(TEST_ROLE_ID)).thenReturn(testRoleEntity);

      // When
      ResponseDTO<RoleVO> result = roleService.getRoleById(TEST_ROLE_ID);

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertInstanceOf(RoleVO.class, result.getData());
    }
  }

  @Nested
  @DisplayName("getAllRole() Tests - List Retrieval")
  class GetAllRoleTests {

    @Test
    @DisplayName("Should return all roles")
    void getAllRole_HasRoles_ReturnsAll() {
      // Given
      List<RoleEntity> roleEntities = List.of(testRoleEntity, existingRoleEntity);
      when(roleDao.selectList(null)).thenReturn(roleEntities);

      // When
      ResponseDTO<List<RoleVO>> result = roleService.getAllRole();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertEquals(2, result.getData().size());
      verify(roleDao, times(1)).selectList(null);
    }

    @Test
    @DisplayName("Should return empty list when no roles exist")
    void getAllRole_NoRoles_ReturnsEmptyList() {
      // Given
      when(roleDao.selectList(null)).thenReturn(new ArrayList<>());

      // When
      ResponseDTO<List<RoleVO>> result = roleService.getAllRole();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertTrue(result.getData().isEmpty());
      verify(roleDao, times(1)).selectList(null);
    }

    @Test
    @DisplayName("Should convert all entities to VOs")
    void getAllRole_ConvertsEntitiesToVOs() {
      // Given
      List<RoleEntity> roleEntities = List.of(testRoleEntity, existingRoleEntity);
      when(roleDao.selectList(null)).thenReturn(roleEntities);

      // When
      ResponseDTO<List<RoleVO>> result = roleService.getAllRole();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      result.getData().forEach(vo -> assertInstanceOf(RoleVO.class, vo));
    }
  }
}
