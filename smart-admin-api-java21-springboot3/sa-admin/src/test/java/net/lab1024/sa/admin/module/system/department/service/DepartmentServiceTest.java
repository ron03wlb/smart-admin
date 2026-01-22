package net.lab1024.sa.admin.module.system.department.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.admin.module.system.department.domain.form.DepartmentAddForm;
import net.lab1024.sa.admin.module.system.department.domain.form.DepartmentUpdateForm;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentTreeVO;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.admin.module.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.code.UserErrorCode;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * DepartmentService Unit Tests
 *
 * <p>Test Coverage: 7 methods, 25+ test cases
 *
 * <p>Focus Areas: 1. Business rule validation (deleteDepartment constraints) 2. Cache invalidation
 * verification 3. Delegation to DepartmentCacheManager 4. Error handling and edge cases
 *
 * @author Claude Code
 * @since 2026-01-22
 */
@DisplayName("DepartmentService Unit Tests")
class DepartmentServiceTest extends BaseUnitTest {

  @InjectMocks private DepartmentService departmentService;

  @Mock private DepartmentDao departmentDao;

  @Mock private EmployeeDao employeeDao;

  @Mock private DepartmentCacheManager departmentCacheManager;

  // Test constants
  private static final Long TEST_DEPARTMENT_ID = 1001L;
  private static final Long TEST_PARENT_ID = 1000L;
  private static final String TEST_DEPARTMENT_NAME = "Research and Development";
  private static final Integer TEST_SORT = 10;
  private static final Long TEST_MANAGER_ID = 2001L;

  private DepartmentAddForm testAddForm;
  private DepartmentUpdateForm testUpdateForm;
  private DepartmentEntity testDepartmentEntity;
  private DepartmentVO testDepartmentVO;

  @BeforeEach
  void setUp() {
    // Setup test add form
    testAddForm = new DepartmentAddForm();
    testAddForm.setDepartmentName(TEST_DEPARTMENT_NAME);
    testAddForm.setParentId(TEST_PARENT_ID);
    testAddForm.setSort(TEST_SORT);
    testAddForm.setManagerId(TEST_MANAGER_ID);

    // Setup test update form
    testUpdateForm = new DepartmentUpdateForm();
    testUpdateForm.setDepartmentId(TEST_DEPARTMENT_ID);
    testUpdateForm.setDepartmentName(TEST_DEPARTMENT_NAME + " Updated");
    testUpdateForm.setParentId(TEST_PARENT_ID);
    testUpdateForm.setSort(TEST_SORT + 5);
    testUpdateForm.setManagerId(TEST_MANAGER_ID);

    // Setup test department entity
    testDepartmentEntity = new DepartmentEntity();
    testDepartmentEntity.setDepartmentId(TEST_DEPARTMENT_ID);
    testDepartmentEntity.setDepartmentName(TEST_DEPARTMENT_NAME);
    testDepartmentEntity.setParentId(TEST_PARENT_ID);
    testDepartmentEntity.setSort(TEST_SORT);
    testDepartmentEntity.setManagerId(TEST_MANAGER_ID);

    // Setup test department VO
    testDepartmentVO = new DepartmentVO();
    testDepartmentVO.setDepartmentId(TEST_DEPARTMENT_ID);
    testDepartmentVO.setDepartmentName(TEST_DEPARTMENT_NAME);
    testDepartmentVO.setParentId(TEST_PARENT_ID);
    testDepartmentVO.setSort(TEST_SORT);
    testDepartmentVO.setManagerId(TEST_MANAGER_ID);
    testDepartmentVO.setManagerName("Test Manager");
    testDepartmentVO.setCreateTime(LocalDateTime.now());
    testDepartmentVO.setUpdateTime(LocalDateTime.now());
  }

  // ==================== addDepartment() Tests ====================

  @Nested
  @DisplayName("addDepartment() Tests - Department Creation")
  class AddDepartmentTests {

    @Test
    @DisplayName("Should successfully add department")
    void addDepartment_ValidData_Success() {
      // Given
      when(departmentDao.insert(any(DepartmentEntity.class))).thenReturn(1);
      doNothing().when(departmentCacheManager).clearCache();

      // When
      ResponseDTO<String> result = departmentService.addDepartment(testAddForm);

      // Then
      assertOk(result);
      verify(departmentDao, times(1)).insert(any(DepartmentEntity.class));
      verify(departmentCacheManager, times(1)).clearCache();
    }

    @Test
    @DisplayName("Should clear cache after adding department")
    void addDepartment_Success_ClearsCacheCorrectly() {
      // Given
      when(departmentDao.insert(any(DepartmentEntity.class))).thenReturn(1);
      doNothing().when(departmentCacheManager).clearCache();

      // When
      departmentService.addDepartment(testAddForm);

      // Then - verify cache clearing called
      verify(departmentCacheManager, times(1)).clearCache();
    }

    @Test
    @DisplayName("Should handle DAO insert correctly")
    void addDepartment_DaoInsert_CalledWithCorrectData() {
      // Given
      when(departmentDao.insert(any(DepartmentEntity.class))).thenReturn(1);

      // When
      departmentService.addDepartment(testAddForm);

      // Then - verify DAO insert called with correct type
      verify(departmentDao, times(1))
          .insert(
              argThat(
                  (DepartmentEntity entity) ->
                      entity.getDepartmentName().equals(TEST_DEPARTMENT_NAME)
                          && entity.getParentId().equals(TEST_PARENT_ID)
                          && entity.getSort().equals(TEST_SORT)));
    }
  }

  // ==================== updateDepartment() Tests ====================

  @Nested
  @DisplayName("updateDepartment() Tests - Department Update with Validation")
  class UpdateDepartmentTests {

    @Test
    @DisplayName("Should successfully update department")
    void updateDepartment_ValidData_Success() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.updateById(any(DepartmentEntity.class))).thenReturn(1);
      doNothing().when(departmentCacheManager).clearCache();

      // When
      ResponseDTO<String> result = departmentService.updateDepartment(testUpdateForm);

      // Then
      assertOk(result);
      verify(departmentDao, times(1)).selectById(TEST_DEPARTMENT_ID);
      verify(departmentDao, times(1)).updateById(any(DepartmentEntity.class));
      verify(departmentCacheManager, times(1)).clearCache();
    }

    @Test
    @DisplayName("Should reject when parentId is null")
    void updateDepartment_NullParentId_ReturnsError() {
      // Given
      testUpdateForm.setParentId(null);

      // When
      ResponseDTO<String> result = departmentService.updateDepartment(testUpdateForm);

      // Then
      assertErrorContains(result, "父级部门id不能为空");
      verify(departmentDao, never()).selectById(anyLong());
      verify(departmentDao, never()).updateById(any(DepartmentEntity.class));
      verify(departmentCacheManager, never()).clearCache();
    }

    @Test
    @DisplayName("Should reject when department does not exist")
    void updateDepartment_NonExistentDepartment_ReturnsError() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(null);

      // When
      ResponseDTO<String> result = departmentService.updateDepartment(testUpdateForm);

      // Then
      assertError(result, UserErrorCode.DATA_NOT_EXIST);
      verify(departmentDao, times(1)).selectById(TEST_DEPARTMENT_ID);
      verify(departmentDao, never()).updateById(any(DepartmentEntity.class));
      verify(departmentCacheManager, never()).clearCache();
    }

    @Test
    @DisplayName("Should clear cache after successful update")
    void updateDepartment_Success_ClearsCacheCorrectly() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.updateById(any(DepartmentEntity.class))).thenReturn(1);
      doNothing().when(departmentCacheManager).clearCache();

      // When
      departmentService.updateDepartment(testUpdateForm);

      // Then
      verify(departmentCacheManager, times(1)).clearCache();
    }
  }

  // ==================== deleteDepartment() Tests ====================

  @Nested
  @DisplayName("deleteDepartment() Tests - Business Rule Validation")
  class DeleteDepartmentTests {

    @Test
    @DisplayName("Should successfully delete department when no children and no employees")
    void deleteDepartment_ValidDepartment_Success() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.countSubDepartment(TEST_DEPARTMENT_ID)).thenReturn(0); // No children
      when(employeeDao.countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE))
          .thenReturn(0); // No employees
      when(departmentDao.deleteById(TEST_DEPARTMENT_ID)).thenReturn(1);
      doNothing().when(departmentCacheManager).clearCache();

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(TEST_DEPARTMENT_ID);

      // Then
      assertOk(result);
      verify(departmentDao, times(1)).selectById(TEST_DEPARTMENT_ID);
      verify(departmentDao, times(1)).countSubDepartment(TEST_DEPARTMENT_ID);
      verify(employeeDao, times(1)).countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE);
      verify(departmentDao, times(1)).deleteById(TEST_DEPARTMENT_ID);
      verify(departmentCacheManager, times(1)).clearCache();
    }

    @Test
    @DisplayName("Should reject when department does not exist")
    void deleteDepartment_NonExistentDepartment_ReturnsError() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(null);

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(TEST_DEPARTMENT_ID);

      // Then
      assertError(result, UserErrorCode.DATA_NOT_EXIST);
      verify(departmentDao, times(1)).selectById(TEST_DEPARTMENT_ID);
      verify(departmentDao, never()).countSubDepartment(anyLong());
      verify(employeeDao, never()).countByDepartmentId(anyLong(), any(Boolean.class));
      verify(departmentDao, never()).deleteById(anyLong());
      verify(departmentCacheManager, never()).clearCache();
    }

    @Test
    @DisplayName("Should reject when department has sub-departments")
    void deleteDepartment_HasSubDepartments_ReturnsError() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.countSubDepartment(TEST_DEPARTMENT_ID)).thenReturn(3); // Has 3 children

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(TEST_DEPARTMENT_ID);

      // Then
      assertErrorContains(result, "请先删除子级部门");
      verify(departmentDao, times(1)).selectById(TEST_DEPARTMENT_ID);
      verify(departmentDao, times(1)).countSubDepartment(TEST_DEPARTMENT_ID);
      verify(employeeDao, never()).countByDepartmentId(anyLong(), any(Boolean.class));
      verify(departmentDao, never()).deleteById(anyLong());
      verify(departmentCacheManager, never()).clearCache();
    }

    @Test
    @DisplayName("Should reject when department has active employees")
    void deleteDepartment_HasActiveEmployees_ReturnsError() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.countSubDepartment(TEST_DEPARTMENT_ID)).thenReturn(0);
      when(employeeDao.countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE))
          .thenReturn(5); // Has 5 active employees

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(TEST_DEPARTMENT_ID);

      // Then
      assertErrorContains(result, "请先删除部门员工");
      verify(departmentDao, times(1)).selectById(TEST_DEPARTMENT_ID);
      verify(departmentDao, times(1)).countSubDepartment(TEST_DEPARTMENT_ID);
      verify(employeeDao, times(1)).countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE);
      verify(departmentDao, never()).deleteById(anyLong());
      verify(departmentCacheManager, never()).clearCache();
    }

    @Test
    @DisplayName("Should clear cache after successful deletion")
    void deleteDepartment_Success_ClearsCacheCorrectly() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.countSubDepartment(TEST_DEPARTMENT_ID)).thenReturn(0);
      when(employeeDao.countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE)).thenReturn(0);
      when(departmentDao.deleteById(TEST_DEPARTMENT_ID)).thenReturn(1);
      doNothing().when(departmentCacheManager).clearCache();

      // When
      departmentService.deleteDepartment(TEST_DEPARTMENT_ID);

      // Then - verify cache clearing called
      verify(departmentCacheManager, times(1)).clearCache();
    }

    @Test
    @DisplayName("Should perform business rule checks in correct order")
    void deleteDepartment_BusinessRuleOrder_ChecksPerformedSequentially() {
      // Given
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentEntity);
      when(departmentDao.countSubDepartment(TEST_DEPARTMENT_ID)).thenReturn(0);
      when(employeeDao.countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE)).thenReturn(0);
      when(departmentDao.deleteById(TEST_DEPARTMENT_ID)).thenReturn(1);

      // When
      departmentService.deleteDepartment(TEST_DEPARTMENT_ID);

      // Then - verify checks performed in order: exist check → sub-dept check → employee check →
      // delete
      var inOrder = inOrder(departmentDao, employeeDao, departmentCacheManager);
      inOrder.verify(departmentDao).selectById(TEST_DEPARTMENT_ID);
      inOrder.verify(departmentDao).countSubDepartment(TEST_DEPARTMENT_ID);
      inOrder.verify(employeeDao).countByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE);
      inOrder.verify(departmentDao).deleteById(TEST_DEPARTMENT_ID);
      inOrder.verify(departmentCacheManager).clearCache();
    }
  }

  // ==================== departmentTree() Tests ====================

  @Nested
  @DisplayName("departmentTree() Tests - Delegation to Cache Manager")
  class DepartmentTreeTests {

    @Test
    @DisplayName("Should delegate to cache manager and return tree structure")
    void departmentTree_DelegatesToCacheManager_ReturnsTree() {
      // Given
      List<DepartmentTreeVO> mockTree = createMockDepartmentTree();
      when(departmentCacheManager.getDepartmentTree()).thenReturn(mockTree);

      // When
      ResponseDTO<List<DepartmentTreeVO>> result = departmentService.departmentTree();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertEquals(mockTree, result.getData());
      verify(departmentCacheManager, times(1)).getDepartmentTree();
    }

    @Test
    @DisplayName("Should handle empty tree from cache manager")
    void departmentTree_EmptyTree_ReturnsEmptyList() {
      // Given
      when(departmentCacheManager.getDepartmentTree()).thenReturn(Lists.newArrayList());

      // When
      ResponseDTO<List<DepartmentTreeVO>> result = departmentService.departmentTree();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertTrue(result.getData().isEmpty());
      verify(departmentCacheManager, times(1)).getDepartmentTree();
    }
  }

  // ==================== selfAndChildrenIdList() Tests ====================

  @Nested
  @DisplayName("selfAndChildrenIdList() Tests - Delegation to Cache Manager")
  class SelfAndChildrenIdListTests {

    @Test
    @DisplayName("Should delegate to cache manager and return ID list")
    void selfAndChildrenIdList_DelegatesToCacheManager_ReturnsIdList() {
      // Given
      List<Long> mockIdList = Lists.newArrayList(1001L, 1002L, 1003L, 1004L);
      when(departmentCacheManager.getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID))
          .thenReturn(mockIdList);

      // When
      List<Long> result = departmentService.selfAndChildrenIdList(TEST_DEPARTMENT_ID);

      // Then
      assertNotNull(result);
      assertEquals(4, result.size());
      assertEquals(mockIdList, result);
      verify(departmentCacheManager, times(1)).getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should handle leaf department (no children)")
    void selfAndChildrenIdList_LeafDepartment_ReturnsSelfOnly() {
      // Given - leaf department returns only itself
      List<Long> mockIdList = Lists.newArrayList(TEST_DEPARTMENT_ID);
      when(departmentCacheManager.getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID))
          .thenReturn(mockIdList);

      // When
      List<Long> result = departmentService.selfAndChildrenIdList(TEST_DEPARTMENT_ID);

      // Then
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(TEST_DEPARTMENT_ID, result.get(0));
      verify(departmentCacheManager, times(1)).getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID);
    }
  }

  // ==================== listAll() Tests ====================

  @Nested
  @DisplayName("listAll() Tests - Delegation to Cache Manager")
  class ListAllTests {

    @Test
    @DisplayName("Should delegate to cache manager and return all departments")
    void listAll_DelegatesToCacheManager_ReturnsAllDepartments() {
      // Given
      List<DepartmentVO> mockDepartments = createMockDepartmentList();
      when(departmentCacheManager.getDepartmentList()).thenReturn(mockDepartments);

      // When
      List<DepartmentVO> result = departmentService.listAll();

      // Then
      assertNotNull(result);
      assertEquals(3, result.size());
      assertEquals(mockDepartments, result);
      verify(departmentCacheManager, times(1)).getDepartmentList();
    }

    @Test
    @DisplayName("Should handle empty department list")
    void listAll_EmptyList_ReturnsEmptyList() {
      // Given
      when(departmentCacheManager.getDepartmentList()).thenReturn(Lists.newArrayList());

      // When
      List<DepartmentVO> result = departmentService.listAll();

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(departmentCacheManager, times(1)).getDepartmentList();
    }
  }

  // ==================== getDepartmentById() Tests ====================

  @Nested
  @DisplayName("getDepartmentById() Tests - Direct DAO Retrieval")
  class GetDepartmentByIdTests {

    @Test
    @DisplayName("Should return department when exists")
    void getDepartmentById_ExistingDepartment_ReturnsDepartmentVO() {
      // Given
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentVO);

      // When
      DepartmentVO result = departmentService.getDepartmentById(TEST_DEPARTMENT_ID);

      // Then
      assertNotNull(result);
      assertEquals(TEST_DEPARTMENT_ID, result.getDepartmentId());
      assertEquals(TEST_DEPARTMENT_NAME, result.getDepartmentName());
      verify(departmentDao, times(1)).selectDepartmentVO(TEST_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should return null when department does not exist")
    void getDepartmentById_NonExistentDepartment_ReturnsNull() {
      // Given
      when(departmentDao.selectDepartmentVO(999L)).thenReturn(null);

      // When
      DepartmentVO result = departmentService.getDepartmentById(999L);

      // Then
      assertNull(result);
      verify(departmentDao, times(1)).selectDepartmentVO(999L);
    }

    @Test
    @DisplayName("Should directly call DAO without cache manager")
    void getDepartmentById_DirectDaoCall_NoCacheManagerInvolved() {
      // Given
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartmentVO);

      // When
      departmentService.getDepartmentById(TEST_DEPARTMENT_ID);

      // Then - verify cache manager NOT called for this method
      verify(departmentDao, times(1)).selectDepartmentVO(TEST_DEPARTMENT_ID);
      verifyNoInteractions(departmentCacheManager);
    }
  }

  // ==================== getDepartmentPath() Tests ====================

  @Nested
  @DisplayName("getDepartmentPath() Tests - Delegation to Cache Manager")
  class GetDepartmentPathTests {

    @Test
    @DisplayName("Should delegate to cache manager and return path")
    void getDepartmentPath_DelegatesToCacheManager_ReturnsPath() {
      // Given
      String expectedPath = "/公司/研发部/后端组";
      Map<Long, String> mockPathMap = new HashMap<>();
      mockPathMap.put(TEST_DEPARTMENT_ID, expectedPath);
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(mockPathMap);

      // When
      String result = departmentService.getDepartmentPath(TEST_DEPARTMENT_ID);

      // Then
      assertNotNull(result);
      assertEquals(expectedPath, result);
      verify(departmentCacheManager, times(1)).getDepartmentPathMap();
    }

    @Test
    @DisplayName("Should return root path for root department")
    void getDepartmentPath_RootDepartment_ReturnsRootPath() {
      // Given
      String expectedPath = "/公司";
      Map<Long, String> mockPathMap = new HashMap<>();
      mockPathMap.put(1L, expectedPath);
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(mockPathMap);

      // When
      String result = departmentService.getDepartmentPath(1L);

      // Then
      assertEquals(expectedPath, result);
      verify(departmentCacheManager, times(1)).getDepartmentPathMap();
    }

    @Test
    @DisplayName("Should handle non-existent department (null path)")
    void getDepartmentPath_NonExistentDepartment_ReturnsNull() {
      // Given
      Map<Long, String> mockPathMap = new HashMap<>();
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(mockPathMap);

      // When
      String result = departmentService.getDepartmentPath(999L);

      // Then
      assertNull(result);
      verify(departmentCacheManager, times(1)).getDepartmentPathMap();
    }
  }

  // ==================== Test Data Helpers ====================

  /** Creates mock department tree for testing */
  private List<DepartmentTreeVO> createMockDepartmentTree() {
    DepartmentTreeVO root = new DepartmentTreeVO();
    root.setDepartmentId(1L);
    root.setDepartmentName("Company");
    root.setParentId(0L);

    DepartmentTreeVO child1 = new DepartmentTreeVO();
    child1.setDepartmentId(2L);
    child1.setDepartmentName("R&D Department");
    child1.setParentId(1L);

    root.setChildren(Lists.newArrayList(child1));

    return Lists.newArrayList(root);
  }

  /** Creates mock department list for testing */
  private List<DepartmentVO> createMockDepartmentList() {
    DepartmentVO dept1 = new DepartmentVO();
    dept1.setDepartmentId(1L);
    dept1.setDepartmentName("Company");

    DepartmentVO dept2 = new DepartmentVO();
    dept2.setDepartmentId(2L);
    dept2.setDepartmentName("R&D");

    DepartmentVO dept3 = new DepartmentVO();
    dept3.setDepartmentId(3L);
    dept3.setDepartmentName("Marketing");

    return Lists.newArrayList(dept1, dept2, dept3);
  }
}
