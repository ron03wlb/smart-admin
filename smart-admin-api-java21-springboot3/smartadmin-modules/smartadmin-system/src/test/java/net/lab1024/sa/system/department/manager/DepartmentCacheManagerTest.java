package net.lab1024.sa.system.department.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.common.cache.CacheService;
import net.lab1024.sa.common.cache.constant.CacheKeyConst;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.vo.DepartmentTreeVO;
import net.lab1024.sa.system.department.domain.vo.DepartmentVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DepartmentCacheManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>緩存清除操作
 *   <li>部門列表查詢
 *   <li>部門樹構建
 *   <li>部門路徑構建
 *   <li>自身及子部門查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentCacheManager 單元測試")
class DepartmentCacheManagerTest {

  @Mock private DepartmentDao departmentDao;

  @Mock private CacheService cacheService;

  @InjectMocks private DepartmentCacheManager departmentCacheManager;

  // ==================== clearCache 測試 ====================

  @Nested
  @DisplayName("clearCache 清除緩存測試")
  class ClearCacheTest {

    @Test
    @DisplayName("正常情況：應該清除所有部門相關緩存")
    void shouldClearAllDepartmentCache() {
      // When
      departmentCacheManager.clearCache();

      // Then
      verify(cacheService).clear(CacheKeyConst.Department.DEPARTMENT_LIST_CACHE);
      verify(cacheService).clear(CacheKeyConst.Department.DEPARTMENT_TREE_CACHE);
      verify(cacheService).clear(CacheKeyConst.Department.DEPARTMENT_SELF_CHILDREN_CACHE);
      verify(cacheService).clear(CacheKeyConst.Department.DEPARTMENT_PATH_CACHE);
    }
  }

  // ==================== getDepartmentList 測試 ====================

  @Nested
  @DisplayName("getDepartmentList 查詢部門列表測試")
  class GetDepartmentListTest {

    @Test
    @DisplayName("正常情況：應該返回部門列表")
    void shouldReturnDepartmentList() {
      // Given
      DepartmentVO vo1 = createTestDepartmentVO(1L, "Dept1", 0L);
      DepartmentVO vo2 = createTestDepartmentVO(2L, "Dept2", 0L);

      when(departmentDao.listAll()).thenReturn(Arrays.asList(vo1, vo2));

      // When
      List<DepartmentVO> result = departmentCacheManager.getDepartmentList();

      // Then
      assertThat(result).hasSize(2);
    }
  }

  // ==================== getDepartmentTree 測試 ====================

  @Nested
  @DisplayName("getDepartmentTree 查詢部門樹測試")
  class GetDepartmentTreeTest {

    @Test
    @DisplayName("正常情況：應該構建部門樹結構")
    void shouldBuildDepartmentTree() {
      // Given
      DepartmentVO root = createTestDepartmentVO(1L, "Root", 0L);
      DepartmentVO child = createTestDepartmentVO(2L, "Child", 1L);

      when(departmentDao.listAll()).thenReturn(Arrays.asList(root, child));

      // When
      List<DepartmentTreeVO> result = departmentCacheManager.getDepartmentTree();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getDepartmentName()).isEqualTo("Root");
      assertThat(result.get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("邊界情況：無數據時返回空列表")
    void shouldReturnEmptyWhenNoData() {
      // Given
      when(departmentDao.listAll()).thenReturn(Collections.emptyList());

      // When
      List<DepartmentTreeVO> result = departmentCacheManager.getDepartmentTree();

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== getDepartmentSelfAndChildren 測試 ====================

  @Nested
  @DisplayName("getDepartmentSelfAndChildren 查詢自身及子部門測試")
  class GetDepartmentSelfAndChildrenTest {

    @Test
    @DisplayName("正常情況：應該返回自身及子部門 ID 列表")
    void shouldReturnSelfAndChildrenIds() {
      // Given
      Long departmentId = 1L;
      DepartmentVO parent = createTestDepartmentVO(1L, "Parent", 0L);
      DepartmentVO child1 = createTestDepartmentVO(2L, "Child1", 1L);
      DepartmentVO child2 = createTestDepartmentVO(3L, "Child2", 1L);

      when(departmentDao.listAll()).thenReturn(Arrays.asList(parent, child1, child2));

      // When
      List<Long> result = departmentCacheManager.getDepartmentSelfAndChildren(departmentId);

      // Then
      assertThat(result).contains(1L, 2L, 3L);
    }

    @Test
    @DisplayName("邊界情況：無子部門時只返回自己")
    void shouldReturnOnlySelfWhenNoChildren() {
      // Given
      Long departmentId = 1L;
      DepartmentVO dept = createTestDepartmentVO(1L, "Dept", 0L);

      when(departmentDao.listAll()).thenReturn(Collections.singletonList(dept));

      // When
      List<Long> result = departmentCacheManager.getDepartmentSelfAndChildren(departmentId);

      // Then
      assertThat(result).containsExactly(1L);
    }
  }

  // ==================== getDepartmentPathMap 測試 ====================

  @Nested
  @DisplayName("getDepartmentPathMap 查詢部門路徑測試")
  class GetDepartmentPathMapTest {

    @Test
    @DisplayName("正常情況：應該返回部門路徑 Map")
    void shouldReturnDepartmentPathMap() {
      // Given
      DepartmentVO root = createTestDepartmentVO(1L, "公司", 0L);
      DepartmentVO child = createTestDepartmentVO(2L, "研發部", 1L);

      when(departmentDao.listAll()).thenReturn(Arrays.asList(root, child));

      // When
      Map<Long, String> result = departmentCacheManager.getDepartmentPathMap();

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(1L)).isEqualTo("公司");
      assertThat(result.get(2L)).isEqualTo("公司/研發部");
    }

    @Test
    @DisplayName("邊界情況：根部門路徑只包含自己")
    void shouldReturnSelfNameWhenRoot() {
      // Given
      DepartmentVO root = createTestDepartmentVO(1L, "公司", 0L);

      when(departmentDao.listAll()).thenReturn(Collections.singletonList(root));

      // When
      Map<Long, String> result = departmentCacheManager.getDepartmentPathMap();

      // Then
      assertThat(result.get(1L)).isEqualTo("公司");
    }
  }

  // ==================== buildTree 測試 ====================

  @Nested
  @DisplayName("buildTree 構建樹測試")
  class BuildTreeTest {

    @Test
    @DisplayName("正常情況：應該正確構建多層級樹")
    void shouldBuildMultiLevelTree() {
      // Given
      DepartmentVO root = createTestDepartmentVO(1L, "Root", 0L);
      DepartmentVO level1 = createTestDepartmentVO(2L, "Level1", 1L);
      DepartmentVO level2 = createTestDepartmentVO(3L, "Level2", 2L);

      List<DepartmentVO> voList = Arrays.asList(root, level1, level2);

      // When
      List<DepartmentTreeVO> result = departmentCacheManager.buildTree(voList);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getChildren()).hasSize(1);
      assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("邊界情況：空列表返回空樹")
    void shouldReturnEmptyTreeWhenListEmpty() {
      // When
      List<DepartmentTreeVO> result = departmentCacheManager.buildTree(Collections.emptyList());

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== Helper Methods ====================

  private DepartmentVO createTestDepartmentVO(Long id, String name, Long parentId) {
    DepartmentVO vo = new DepartmentVO();
    vo.setDepartmentId(id);
    vo.setDepartmentName(name);
    vo.setParentId(parentId);
    return vo;
  }
}
