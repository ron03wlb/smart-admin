package net.lab1024.sa.business.category.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.business.category.dao.CategoryDao;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.category.domain.vo.CategoryTreeVO;
import net.lab1024.sa.common.cache.CacheService;
import net.lab1024.sa.common.cache.constant.CacheKeyConst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CategoryCacheManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>緩存清除操作
 *   <li>類目查詢（帶緩存）
 *   <li>子類目查詢
 *   <li>類目樹構建
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryCacheManager 單元測試")
class CategoryCacheManagerTest {

  @Mock private CategoryDao categoryDao;

  @Mock private CacheService cacheService;

  @InjectMocks private CategoryCacheManager categoryCacheManager;

  // ==================== removeCache 測試 ====================

  @Nested
  @DisplayName("removeCache 清除緩存測試")
  class RemoveCacheTest {

    @Test
    @DisplayName("正常情況：應該清除所有類目相關緩存")
    void shouldClearAllCategoryCache() {
      // When
      categoryCacheManager.removeCache();

      // Then
      verify(cacheService).clear(CacheKeyConst.Category.CATEGORY_ENTITY);
      verify(cacheService).clear(CacheKeyConst.Category.CATEGORY_SUB);
      verify(cacheService).clear(CacheKeyConst.Category.CATEGORY_TREE);
    }
  }

  // ==================== queryCategory 測試 ====================

  @Nested
  @DisplayName("queryCategory 查詢類目測試")
  class QueryCategoryTest {

    @Test
    @DisplayName("正常情況：應該返回類目實體")
    void shouldReturnCategoryEntity() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "TestCategory");

      when(categoryDao.selectById(categoryId)).thenReturn(entity);

      // When
      CategoryEntity result = categoryCacheManager.queryCategory(categoryId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getCategoryName()).isEqualTo("TestCategory");
    }

    @Test
    @DisplayName("異常情況：不存在時返回 null")
    void shouldReturnNullWhenNotFound() {
      // Given
      when(categoryDao.selectById(999L)).thenReturn(null);

      // When
      CategoryEntity result = categoryCacheManager.queryCategory(999L);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== querySubCategory 測試 ====================

  @Nested
  @DisplayName("querySubCategory 查詢子類目測試")
  class QuerySubCategoryTest {

    @Test
    @DisplayName("正常情況：應該返回子類目列表")
    void shouldReturnSubCategoryList() {
      // Given
      Long parentId = 1L;
      CategoryEntity sub1 = createTestCategoryEntity(2L, "Sub1");
      CategoryEntity sub2 = createTestCategoryEntity(3L, "Sub2");

      when(categoryDao.queryByParentId(Collections.singletonList(parentId), false))
          .thenReturn(Arrays.asList(sub1, sub2));

      // When
      List<CategoryEntity> result = categoryCacheManager.querySubCategory(parentId);

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("邊界情況：無子類目時返回空列表")
    void shouldReturnEmptyWhenNoChildren() {
      // Given
      Long parentId = 1L;
      when(categoryDao.queryByParentId(Collections.singletonList(parentId), false))
          .thenReturn(Collections.emptyList());

      // When
      List<CategoryEntity> result = categoryCacheManager.querySubCategory(parentId);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== queryCategoryTree 測試 ====================

  @Nested
  @DisplayName("queryCategoryTree 查詢類目樹測試")
  class QueryCategoryTreeTest {

    @Test
    @DisplayName("正常情況：應該構建類目樹結構")
    void shouldBuildCategoryTree() {
      // Given
      Long parentId = 0L;
      Integer categoryType = 1;

      CategoryEntity root = createTestCategoryEntity(1L, "Root");
      root.setParentId(0L);

      CategoryEntity child = createTestCategoryEntity(2L, "Child");
      child.setParentId(1L);

      when(categoryDao.queryByType(categoryType, false)).thenReturn(Arrays.asList(root, child));

      // When
      List<CategoryTreeVO> result = categoryCacheManager.queryCategoryTree(parentId, categoryType);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCategoryName()).isEqualTo("Root");
      assertThat(result.get(0).getChildren()).hasSize(1);
      assertThat(result.get(0).getChildren().get(0).getCategoryName()).isEqualTo("Child");
    }

    @Test
    @DisplayName("邊界情況：無數據時返回空列表")
    void shouldReturnEmptyWhenNoData() {
      // Given
      when(categoryDao.queryByType(1, false)).thenReturn(Collections.emptyList());

      // When
      List<CategoryTreeVO> result = categoryCacheManager.queryCategoryTree(0L, 1);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("正常情況：應該設置 label 和 value 字段")
    void shouldSetLabelAndValue() {
      // Given
      CategoryEntity root = createTestCategoryEntity(1L, "Root");
      root.setParentId(0L);

      when(categoryDao.queryByType(1, false)).thenReturn(Collections.singletonList(root));

      // When
      List<CategoryTreeVO> result = categoryCacheManager.queryCategoryTree(0L, 1);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getLabel()).isEqualTo("Root");
      assertThat(result.get(0).getValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("正常情況：應該設置 categoryFullName")
    void shouldSetCategoryFullName() {
      // Given
      CategoryEntity root = createTestCategoryEntity(1L, "Root");
      root.setParentId(0L);

      CategoryEntity child = createTestCategoryEntity(2L, "Child");
      child.setParentId(1L);

      when(categoryDao.queryByType(1, false)).thenReturn(Arrays.asList(root, child));

      // When
      List<CategoryTreeVO> result = categoryCacheManager.queryCategoryTree(0L, 1);

      // Then
      assertThat(result.get(0).getCategoryFullName()).isEqualTo("Root");
      assertThat(result.get(0).getChildren().get(0).getCategoryFullName()).isEqualTo("Root/Child");
    }
  }

  // ==================== Helper Methods ====================

  private CategoryEntity createTestCategoryEntity(Long id, String name) {
    CategoryEntity entity = new CategoryEntity();
    entity.setCategoryId(id);
    entity.setCategoryName(name);
    entity.setCategoryType(1);
    entity.setParentId(0L);
    entity.setDeletedFlag(false);
    return entity;
  }
}
