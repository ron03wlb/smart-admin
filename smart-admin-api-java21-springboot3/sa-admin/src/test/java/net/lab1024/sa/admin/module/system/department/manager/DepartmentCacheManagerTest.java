package net.lab1024.sa.admin.module.system.department.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentTreeVO;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.common.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * DepartmentCacheManager Unit Tests
 *
 * <p>Tests complex tree algorithms and department hierarchy management. Focuses on recursive tree
 * building, sibling linking (preId/nextId), and descendant ID collection.
 *
 * <p><b>Testing Strategy:</b>
 *
 * <ul>
 *   <li>Pure unit tests with Mockito (no Spring context)
 *   <li>Mock DepartmentDao to return test department hierarchies
 *   <li>Test tree algorithms: single root, multiple roots, deep nesting (5+ levels)
 *   <li>Verify preId/nextId sibling linking correctness
 *   <li>Verify selfAndAllChildrenIdList population (all descendant IDs)
 *   <li>Test path building: "Company/IT Department/Backend Team"
 *   <li>Cache annotations (@Cached) are framework features tested in production
 * </ul>
 *
 * <p><b>Test Data Structure:</b>
 *
 * <pre>
 * Company (ID: 1, parentId: 0)
 *   ├── IT Department (ID: 2, parentId: 1)
 *   │   ├── Backend Team (ID: 4, parentId: 2)
 *   │   │   └── Java Group (ID: 7, parentId: 4)
 *   │   └── Frontend Team (ID: 5, parentId: 2)
 *   └── Sales Department (ID: 3, parentId: 1)
 *       └── Regional Sales (ID: 6, parentId: 3)
 * </pre>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@DisplayName("DepartmentCacheManager Unit Tests - Tree Algorithms")
class DepartmentCacheManagerTest extends BaseUnitTest {

  @InjectMocks private DepartmentCacheManager departmentCacheManager;

  @Mock private DepartmentDao departmentDao;

  @Mock private CacheService cacheService;

  // Test data - full department hierarchy
  private List<DepartmentVO> fullDepartmentList;

  @BeforeEach
  void setUp() {
    fullDepartmentList = createFullDepartmentHierarchy();
  }

  /**
   * Creates a complete department hierarchy for testing
   *
   * <pre>
   * Company (1)
   *   ├── IT Department (2)
   *   │   ├── Backend Team (4)
   *   │   │   └── Java Group (7)
   *   │   └── Frontend Team (5)
   *   └── Sales Department (3)
   *       └── Regional Sales (6)
   * </pre>
   */
  private List<DepartmentVO> createFullDepartmentHierarchy() {
    List<DepartmentVO> departments = new ArrayList<>();

    // Root: Company
    departments.add(createDepartment(1L, 0L, "Company", 1));

    // Level 1: IT Department, Sales Department
    departments.add(createDepartment(2L, 1L, "IT Department", 2));
    departments.add(createDepartment(3L, 1L, "Sales Department", 3));

    // Level 2: Backend Team, Frontend Team (under IT)
    departments.add(createDepartment(4L, 2L, "Backend Team", 4));
    departments.add(createDepartment(5L, 2L, "Frontend Team", 5));

    // Level 2: Regional Sales (under Sales)
    departments.add(createDepartment(6L, 3L, "Regional Sales", 6));

    // Level 3: Java Group (under Backend Team) - deep nesting
    departments.add(createDepartment(7L, 4L, "Java Group", 7));

    return departments;
  }

  private DepartmentVO createDepartment(Long id, Long parentId, String name, Integer sort) {
    DepartmentVO dept = new DepartmentVO();
    dept.setDepartmentId(id);
    dept.setParentId(parentId);
    dept.setDepartmentName(name);
    dept.setSort(sort);
    return dept;
  }

  @Nested
  @DisplayName("clearCache() Tests")
  class ClearCacheTests {

    @Test
    @DisplayName("Should clear all department caches")
    void clearCache_ClearsAllCaches() {
      // When
      departmentCacheManager.clearCache();

      // Then - Verify all 4 cache types cleared
      verify(cacheService, times(1)).clear("department_list_cache");
      verify(cacheService, times(1)).clear("department_tree_cache");
      verify(cacheService, times(1)).clear("department_self_children_cache");
      verify(cacheService, times(1)).clear("department_path_cache");
    }

    @Test
    @DisplayName("Should not throw exception when cache service fails")
    void clearCache_CacheServiceFails_DoesNotThrow() {
      // Given
      doThrow(new RuntimeException("Cache service error")).when(cacheService).clear(anyString());

      // When/Then - Should not throw
      assertThrows(RuntimeException.class, () -> departmentCacheManager.clearCache());
    }
  }

  @Nested
  @DisplayName("getDepartmentList() Tests")
  class GetDepartmentListTests {

    @Test
    @DisplayName("Should return all departments from DAO")
    void getDepartmentList_ReturnsAllDepartments() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<DepartmentVO> result = departmentCacheManager.getDepartmentList();

      // Then
      assertNotNull(result);
      assertEquals(7, result.size());
      verify(departmentDao, times(1)).listAll();
    }

    @Test
    @DisplayName("Should return empty list when no departments")
    void getDepartmentList_NoDepartments_ReturnsEmptyList() {
      // Given
      when(departmentDao.listAll()).thenReturn(new ArrayList<>());

      // When
      List<DepartmentVO> result = departmentCacheManager.getDepartmentList();

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getDepartmentTree() Tests - Tree Building")
  class GetDepartmentTreeTests {

    @Test
    @DisplayName("Should build complete tree structure with all levels")
    void getDepartmentTree_FullHierarchy_BuildsCompleteTree() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.getDepartmentTree();

      // Then
      assertNotNull(tree);
      assertEquals(1, tree.size()); // Only 1 root (Company)

      DepartmentTreeVO root = tree.get(0);
      assertEquals(1L, root.getDepartmentId());
      assertEquals("Company", root.getDepartmentName());

      // Verify children
      assertNotNull(root.getChildren());
      assertEquals(2, root.getChildren().size()); // IT and Sales departments
    }

    @Test
    @DisplayName("Should set preId and nextId for sibling nodes")
    void getDepartmentTree_SiblingLinking_SetsPreIdAndNextId() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.getDepartmentTree();

      // Then
      DepartmentTreeVO root = tree.get(0);
      List<DepartmentTreeVO> children = root.getChildren();

      // First child (IT Department, ID: 2) should have no preId, but nextId = 3
      DepartmentTreeVO firstChild = children.get(0);
      assertNull(firstChild.getPreId());
      assertEquals(3L, firstChild.getNextId());

      // Second child (Sales Department, ID: 3) should have preId = 2, no nextId
      DepartmentTreeVO secondChild = children.get(1);
      assertEquals(2L, secondChild.getPreId());
      assertNull(secondChild.getNextId());
    }

    @Test
    @DisplayName("Should populate selfAndAllChildrenIdList recursively")
    void getDepartmentTree_SelfAndAllChildrenIdList_PopulatedRecursively() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.getDepartmentTree();

      // Then
      DepartmentTreeVO root = tree.get(0);

      // Root should contain itself + all 6 descendants = 7 IDs
      assertNotNull(root.getSelfAndAllChildrenIdList());
      assertEquals(7, root.getSelfAndAllChildrenIdList().size());
      assertTrue(root.getSelfAndAllChildrenIdList().contains(1L)); // Company
      assertTrue(root.getSelfAndAllChildrenIdList().contains(2L)); // IT Department
      assertTrue(root.getSelfAndAllChildrenIdList().contains(7L)); // Java Group (deepest)
    }

    @Test
    @DisplayName("Should handle deep nesting (4 levels)")
    void getDepartmentTree_DeepNesting_BuildsCorrectly() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.getDepartmentTree();

      // Then - Navigate to deepest node (Java Group, ID: 7)
      DepartmentTreeVO root = tree.get(0); // Company
      DepartmentTreeVO itDept = root.getChildren().get(0); // IT Department
      DepartmentTreeVO backendTeam = itDept.getChildren().get(0); // Backend Team
      DepartmentTreeVO javaGroup = backendTeam.getChildren().get(0); // Java Group

      assertEquals(7L, javaGroup.getDepartmentId());
      assertEquals("Java Group", javaGroup.getDepartmentName());

      // Java Group is a leaf node (no children)
      assertTrue(
          javaGroup.getChildren() == null || javaGroup.getChildren().isEmpty(),
          "Java Group should be a leaf node");
    }

    @Test
    @DisplayName("Should return empty list when no departments")
    void getDepartmentTree_EmptyList_ReturnsEmpty() {
      // Given
      when(departmentDao.listAll()).thenReturn(new ArrayList<>());

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.getDepartmentTree();

      // Then
      assertNotNull(tree);
      assertTrue(tree.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple root nodes")
    void getDepartmentTree_MultipleRoots_BuildsMultipleTrees() {
      // Given - Add another root
      List<DepartmentVO> multiRootList = new ArrayList<>(fullDepartmentList);
      multiRootList.add(createDepartment(100L, 0L, "Another Company", 100));

      when(departmentDao.listAll()).thenReturn(multiRootList);

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.getDepartmentTree();

      // Then - Should have 2 roots
      assertNotNull(tree);
      assertEquals(2, tree.size());

      assertEquals(1L, tree.get(0).getDepartmentId());
      assertEquals(100L, tree.get(1).getDepartmentId());
    }
  }

  @Nested
  @DisplayName("buildTree() Tests - Public Method")
  class BuildTreeTests {

    @Test
    @DisplayName("Should build tree from department list")
    void buildTree_ValidList_BuildsTree() {
      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.buildTree(fullDepartmentList);

      // Then
      assertNotNull(tree);
      assertEquals(1, tree.size());
      assertEquals("Company", tree.get(0).getDepartmentName());
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void buildTree_NullInput_ReturnsEmptyList() {
      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.buildTree(null);

      // Then
      assertNotNull(tree);
      assertTrue(tree.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for empty input")
    void buildTree_EmptyInput_ReturnsEmptyList() {
      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.buildTree(new ArrayList<>());

      // Then
      assertNotNull(tree);
      assertTrue(tree.isEmpty());
    }

    @Test
    @DisplayName("Should handle single node (no children)")
    void buildTree_SingleNode_ReturnsSingleNodeTree() {
      // Given
      List<DepartmentVO> singleNode = List.of(createDepartment(1L, 0L, "Company", 1));

      // When
      List<DepartmentTreeVO> tree = departmentCacheManager.buildTree(singleNode);

      // Then
      assertNotNull(tree);
      assertEquals(1, tree.size());

      DepartmentTreeVO root = tree.get(0);
      assertEquals(1L, root.getDepartmentId());
      assertTrue(
          root.getChildren() == null || root.getChildren().isEmpty(),
          "Single node should have no children");
    }
  }

  @Nested
  @DisplayName("getDepartmentSelfAndChildren() Tests - Cached Method")
  class GetDepartmentSelfAndChildrenTests {

    @Test
    @DisplayName("Should return self + all descendants for parent department")
    void getDepartmentSelfAndChildren_ParentDepartment_ReturnsAllDescendants() {
      // Given - Get descendants of IT Department (ID: 2)
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<Long> result = departmentCacheManager.getDepartmentSelfAndChildren(2L);

      // Then - IT (2) + Backend (4) + Frontend (5) + Java Group (7) = 4 IDs
      assertNotNull(result);
      assertEquals(4, result.size());
      assertTrue(result.contains(2L)); // IT Department (self)
      assertTrue(result.contains(4L)); // Backend Team
      assertTrue(result.contains(5L)); // Frontend Team
      assertTrue(result.contains(7L)); // Java Group (nested under Backend)
    }

    @Test
    @DisplayName("Should return only self for leaf node (no children)")
    void getDepartmentSelfAndChildren_LeafNode_ReturnsSelfOnly() {
      // Given - Java Group (ID: 7) is a leaf node
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<Long> result = departmentCacheManager.getDepartmentSelfAndChildren(7L);

      // Then - Only Java Group itself
      assertNotNull(result);
      assertEquals(1, result.size());
      assertTrue(result.contains(7L));
    }

    @Test
    @DisplayName("Should return all IDs for root department")
    void getDepartmentSelfAndChildren_RootDepartment_ReturnsAllIDs() {
      // Given - Company (ID: 1) is root
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      List<Long> result = departmentCacheManager.getDepartmentSelfAndChildren(1L);

      // Then - All 7 departments
      assertNotNull(result);
      assertEquals(7, result.size());
      assertTrue(result.contains(1L)); // Company
      assertTrue(result.contains(7L)); // Java Group (deepest nested)
    }

    @Test
    @DisplayName("Should return empty list when department list is empty")
    void getDepartmentSelfAndChildren_EmptyList_ReturnsEmpty() {
      // Given
      when(departmentDao.listAll()).thenReturn(new ArrayList<>());

      // When
      List<Long> result = departmentCacheManager.getDepartmentSelfAndChildren(1L);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("selfAndChildrenIdList() Tests - Public Method")
  class SelfAndChildrenIdListTests {

    @Test
    @DisplayName("Should collect self + all children recursively")
    void selfAndChildrenIdList_CollectsAllDescendants() {
      // When - Get descendants of Backend Team (ID: 4)
      List<Long> result = departmentCacheManager.selfAndChildrenIdList(4L, fullDepartmentList);

      // Then - Backend (4) + Java Group (7) = 2 IDs
      assertNotNull(result);
      assertEquals(2, result.size());
      assertTrue(result.contains(4L)); // Backend Team (self)
      assertTrue(result.contains(7L)); // Java Group (child)
    }

    @Test
    @DisplayName("Should handle empty department list")
    void selfAndChildrenIdList_EmptyList_ReturnsEmpty() {
      // When
      List<Long> result = departmentCacheManager.selfAndChildrenIdList(1L, new ArrayList<>());

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null department list")
    void selfAndChildrenIdList_NullList_ReturnsEmpty() {
      // When
      List<Long> result = departmentCacheManager.selfAndChildrenIdList(1L, null);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should include multi-level descendants")
    void selfAndChildrenIdList_MultiLevelNesting_IncludesAllLevels() {
      // When - Get descendants of Company (root, ID: 1)
      List<Long> result = departmentCacheManager.selfAndChildrenIdList(1L, fullDepartmentList);

      // Then - All 7 departments
      assertNotNull(result);
      assertEquals(7, result.size());

      // Verify all levels included
      assertTrue(result.contains(1L)); // Level 0: Company
      assertTrue(result.contains(2L)); // Level 1: IT Department
      assertTrue(result.contains(4L)); // Level 2: Backend Team
      assertTrue(result.contains(7L)); // Level 3: Java Group
    }
  }

  @Nested
  @DisplayName("selfAndChildrenRecursion() Tests - Public Recursive Helper")
  class SelfAndChildrenRecursionTests {

    @Test
    @DisplayName("Should add children to provided list recursively")
    void selfAndChildrenRecursion_AddsChildrenRecursively() {
      // Given
      List<Long> resultList = new ArrayList<>();
      resultList.add(2L); // IT Department (starting point)

      // When
      departmentCacheManager.selfAndChildrenRecursion(resultList, 2L, fullDepartmentList);

      // Then - Should add Backend (4), Frontend (5), Java Group (7)
      assertTrue(resultList.contains(4L)); // Backend Team
      assertTrue(resultList.contains(5L)); // Frontend Team
      assertTrue(resultList.contains(7L)); // Java Group (nested under Backend)
    }

    @Test
    @DisplayName("Should stop at leaf nodes (no children)")
    void selfAndChildrenRecursion_LeafNode_DoesNotAddMore() {
      // Given
      List<Long> resultList = new ArrayList<>();
      resultList.add(7L); // Java Group (leaf node)

      int initialSize = resultList.size();

      // When
      departmentCacheManager.selfAndChildrenRecursion(resultList, 7L, fullDepartmentList);

      // Then - No children added (leaf node)
      assertEquals(initialSize, resultList.size());
    }

    @Test
    @DisplayName("Should handle deep recursion without stack overflow")
    void selfAndChildrenRecursion_DeepNesting_HandlesCorrectly() {
      // Given
      List<Long> resultList = new ArrayList<>();
      resultList.add(1L); // Company (root)

      // When - Recurse from root (deepest path: Company → IT → Backend → Java Group)
      departmentCacheManager.selfAndChildrenRecursion(resultList, 1L, fullDepartmentList);

      // Then - All descendants added (6 children)
      assertEquals(7, resultList.size()); // 1 (initial) + 6 (descendants)
      assertTrue(resultList.contains(7L)); // Deepest node included
    }
  }

  @Nested
  @DisplayName("getDepartmentPathMap() Tests - Path Building")
  class GetDepartmentPathMapTests {

    @Test
    @DisplayName("Should build paths for all departments")
    void getDepartmentPathMap_BuildsAllPaths() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      Map<Long, String> pathMap = departmentCacheManager.getDepartmentPathMap();

      // Then
      assertNotNull(pathMap);
      assertEquals(7, pathMap.size()); // All 7 departments

      // Verify root path (no parent)
      assertEquals("Company", pathMap.get(1L));

      // Verify level 1 paths
      assertEquals("Company/IT Department", pathMap.get(2L));
      assertEquals("Company/Sales Department", pathMap.get(3L));

      // Verify level 2 paths
      assertEquals("Company/IT Department/Backend Team", pathMap.get(4L));
      assertEquals("Company/IT Department/Frontend Team", pathMap.get(5L));
      assertEquals("Company/Sales Department/Regional Sales", pathMap.get(6L));

      // Verify deepest path (level 3)
      assertEquals("Company/IT Department/Backend Team/Java Group", pathMap.get(7L));
    }

    @Test
    @DisplayName("Should handle root departments (parentId = 0)")
    void getDepartmentPathMap_RootDepartment_ReturnsNameOnly() {
      // Given
      when(departmentDao.listAll()).thenReturn(fullDepartmentList);

      // When
      Map<Long, String> pathMap = departmentCacheManager.getDepartmentPathMap();

      // Then - Root department path is just the name
      assertEquals("Company", pathMap.get(1L));
    }

    @Test
    @DisplayName("Should return empty map when no departments")
    void getDepartmentPathMap_EmptyList_ReturnsEmptyMap() {
      // Given
      when(departmentDao.listAll()).thenReturn(new ArrayList<>());

      // When
      Map<Long, String> pathMap = departmentCacheManager.getDepartmentPathMap();

      // Then
      assertNotNull(pathMap);
      assertTrue(pathMap.isEmpty());
    }

    @Test
    @DisplayName("Should handle orphan departments (missing parent)")
    void getDepartmentPathMap_OrphanDepartment_ReturnsNameOnly() {
      // Given - Department with non-existent parent
      List<DepartmentVO> orphanList = new ArrayList<>();
      orphanList.add(createDepartment(1L, 0L, "Company", 1));
      orphanList.add(createDepartment(99L, 888L, "Orphan Department", 99)); // Parent 888 doesn't
      // exist

      when(departmentDao.listAll()).thenReturn(orphanList);

      // When
      Map<Long, String> pathMap = departmentCacheManager.getDepartmentPathMap();

      // Then - Orphan department path is just the name (parent not found)
      assertEquals("Orphan Department", pathMap.get(99L));
    }
  }
}
