package net.lab1024.sa.business.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.business.category.domain.dto.CategorySimpleDTO;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.category.manager.CategoryCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CategoryQueryService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>單個類目查詢（Vavr Option）
 *   <li>批量類目查詢
 *   <li>類目名稱查詢
 *   <li>類目全路徑查詢
 *   <li>子類目遞歸查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryQueryService 單元測試")
class CategoryQueryServiceTest {

  @Mock private CategoryCacheManager categoryCacheManager;

  @InjectMocks private CategoryQueryService categoryQueryService;

  // ==================== queryCategory 測試 ====================

  @Nested
  @DisplayName("queryCategory 查詢單個類目測試")
  class QueryCategoryTest {

    @Test
    @DisplayName("正常情況：應該返回 Option.of 包裝的類目")
    void shouldReturnOptionOfCategory() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "TestCategory");

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // When
      Option<CategoryEntity> result = categoryQueryService.queryCategory(categoryId);

      // Then
      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getCategoryName()).isEqualTo("TestCategory");
    }

    @Test
    @DisplayName("異常情況：categoryId 為 null 時應返回 Option.none")
    void shouldReturnNoneWhenIdNull() {
      // When
      Option<CategoryEntity> result = categoryQueryService.queryCategory(null);

      // Then
      assertThat(result.isDefined()).isFalse();
    }

    @Test
    @DisplayName("異常情況：類目不存在時應返回 Option.none")
    void shouldReturnNoneWhenNotFound() {
      // Given
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      Option<CategoryEntity> result = categoryQueryService.queryCategory(999L);

      // Then
      assertThat(result.isDefined()).isFalse();
    }

    @Test
    @DisplayName("異常情況：類目已刪除時應返回 Option.none")
    void shouldReturnNoneWhenDeleted() {
      // Given
      CategoryEntity deletedEntity = createTestCategoryEntity(1L, "Deleted");
      deletedEntity.setDeletedFlag(true);

      when(categoryCacheManager.queryCategory(1L)).thenReturn(deletedEntity);

      // When
      Option<CategoryEntity> result = categoryQueryService.queryCategory(1L);

      // Then
      assertThat(result.isDefined()).isFalse();
    }
  }

  // ==================== queryCategoryList 測試 ====================

  @Nested
  @DisplayName("queryCategoryList 批量查詢測試")
  class QueryCategoryListTest {

    @Test
    @DisplayName("正常情況：應該返回類目 Map")
    void shouldReturnCategoryMap() {
      // Given
      List<Long> categoryIdList = Arrays.asList(1L, 2L);
      CategoryEntity entity1 = createTestCategoryEntity(1L, "Category1");
      CategoryEntity entity2 = createTestCategoryEntity(2L, "Category2");

      when(categoryCacheManager.queryCategory(1L)).thenReturn(entity1);
      when(categoryCacheManager.queryCategory(2L)).thenReturn(entity2);

      // When
      Map<Long, CategoryEntity> result = categoryQueryService.queryCategoryList(categoryIdList);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(1L).getCategoryName()).isEqualTo("Category1");
      assertThat(result.get(2L).getCategoryName()).isEqualTo("Category2");
    }

    @Test
    @DisplayName("邊界情況：空列表應返回空 Map")
    void shouldReturnEmptyMapWhenListEmpty() {
      // When
      Map<Long, CategoryEntity> result =
          categoryQueryService.queryCategoryList(Collections.emptyList());

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("邊界情況：null 列表應返回空 Map")
    void shouldReturnEmptyMapWhenListNull() {
      // When
      Map<Long, CategoryEntity> result = categoryQueryService.queryCategoryList(null);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("正常情況：應該去重後查詢")
    void shouldDeduplicateBeforeQuery() {
      // Given
      List<Long> categoryIdList = Arrays.asList(1L, 1L, 2L, 2L);
      CategoryEntity entity1 = createTestCategoryEntity(1L, "Category1");
      CategoryEntity entity2 = createTestCategoryEntity(2L, "Category2");

      when(categoryCacheManager.queryCategory(1L)).thenReturn(entity1);
      when(categoryCacheManager.queryCategory(2L)).thenReturn(entity2);

      // When
      Map<Long, CategoryEntity> result = categoryQueryService.queryCategoryList(categoryIdList);

      // Then
      assertThat(result).hasSize(2);
    }
  }

  // ==================== queryCategoryName 測試 ====================

  @Nested
  @DisplayName("queryCategoryName 查詢名稱測試")
  class QueryCategoryNameTest {

    @Test
    @DisplayName("正常情況：應該返回類目名稱")
    void shouldReturnCategoryName() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "TestCategory");

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // When
      String result = categoryQueryService.queryCategoryName(categoryId);

      // Then
      assertThat(result).isEqualTo("TestCategory");
    }

    @Test
    @DisplayName("異常情況：類目不存在時返回 null")
    void shouldReturnNullWhenNotFound() {
      // Given
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      String result = categoryQueryService.queryCategoryName(999L);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("正常情況：批量查詢應返回名稱列表")
    void shouldReturnCategoryNameList() {
      // Given
      List<Long> categoryIdList = Arrays.asList(1L, 2L);
      CategoryEntity entity1 = createTestCategoryEntity(1L, "Category1");
      CategoryEntity entity2 = createTestCategoryEntity(2L, "Category2");

      when(categoryCacheManager.queryCategory(1L)).thenReturn(entity1);
      when(categoryCacheManager.queryCategory(2L)).thenReturn(entity2);

      // When
      List<String> result = categoryQueryService.queryCategoryName(categoryIdList);

      // Then
      assertThat(result).containsExactly("Category1", "Category2");
    }
  }

  // ==================== queryCategoryInfo 測試 ====================

  @Nested
  @DisplayName("queryCategoryInfo 查詢詳情測試")
  class QueryCategoryInfoTest {

    @Test
    @DisplayName("正常情況：應該返回類目詳情 DTO")
    void shouldReturnCategoryInfo() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "TestCategory");
      entity.setParentId(0L);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // When
      CategorySimpleDTO result = categoryQueryService.queryCategoryInfo(categoryId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getCategoryId()).isEqualTo(categoryId);
      assertThat(result.getCategoryName()).isEqualTo("TestCategory");
      assertThat(result.getCategoryFullName()).isEqualTo("TestCategory");
    }

    @Test
    @DisplayName("異常情況：類目不存在時返回 null")
    void shouldReturnNullWhenNotFound() {
      // Given
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      CategorySimpleDTO result = categoryQueryService.queryCategoryInfo(999L);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== queryCategoryAndParent 測試 ====================

  @Nested
  @DisplayName("queryCategoryAndParent 查詢父級鏈測試")
  class QueryCategoryAndParentTest {

    @Test
    @DisplayName("正常情況：根類目應只返回自己")
    void shouldReturnSelfWhenRoot() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "Root");
      entity.setParentId(0L);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // When
      List<CategoryEntity> result = categoryQueryService.queryCategoryAndParent(categoryId);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCategoryName()).isEqualTo("Root");
    }

    @Test
    @DisplayName("正常情況：子類目應返回完整父級鏈")
    void shouldReturnFullParentChain() {
      // Given
      CategoryEntity grandParent = createTestCategoryEntity(1L, "GrandParent");
      grandParent.setParentId(0L);

      CategoryEntity parent = createTestCategoryEntity(2L, "Parent");
      parent.setParentId(1L);

      CategoryEntity child = createTestCategoryEntity(3L, "Child");
      child.setParentId(2L);

      when(categoryCacheManager.queryCategory(3L)).thenReturn(child);
      when(categoryCacheManager.queryCategory(2L)).thenReturn(parent);
      when(categoryCacheManager.queryCategory(1L)).thenReturn(grandParent);

      // When
      List<CategoryEntity> result = categoryQueryService.queryCategoryAndParent(3L);

      // Then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getCategoryName()).isEqualTo("GrandParent");
      assertThat(result.get(1).getCategoryName()).isEqualTo("Parent");
      assertThat(result.get(2).getCategoryName()).isEqualTo("Child");
    }

    @Test
    @DisplayName("異常情況：類目不存在時返回空列表")
    void shouldReturnEmptyWhenNotFound() {
      // Given
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      List<CategoryEntity> result = categoryQueryService.queryCategoryAndParent(999L);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== queryFullName 測試 ====================

  @Nested
  @DisplayName("queryFullName 查詢全路徑測試")
  class QueryFullNameTest {

    @Test
    @DisplayName("正常情況：應該返回斜杠分隔的全路徑")
    void shouldReturnSlashSeparatedPath() {
      // Given
      CategoryEntity parent = createTestCategoryEntity(1L, "Parent");
      parent.setParentId(0L);

      CategoryEntity child = createTestCategoryEntity(2L, "Child");
      child.setParentId(1L);

      when(categoryCacheManager.queryCategory(2L)).thenReturn(child);
      when(categoryCacheManager.queryCategory(1L)).thenReturn(parent);

      // When
      String result = categoryQueryService.queryFullName(2L);

      // Then
      assertThat(result).isEqualTo("Parent/Child");
    }

    @Test
    @DisplayName("正常情況：批量查詢應返回 Map")
    void shouldReturnFullNameMap() {
      // Given
      List<Long> categoryIdList = Arrays.asList(1L, 2L);

      CategoryEntity entity1 = createTestCategoryEntity(1L, "Category1");
      entity1.setParentId(0L);

      CategoryEntity entity2 = createTestCategoryEntity(2L, "Category2");
      entity2.setParentId(0L);

      when(categoryCacheManager.queryCategory(1L)).thenReturn(entity1);
      when(categoryCacheManager.queryCategory(2L)).thenReturn(entity2);

      // When
      Map<Long, String> result = categoryQueryService.queryFullName(categoryIdList);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(1L)).isEqualTo("Category1");
      assertThat(result.get(2L)).isEqualTo("Category2");
    }
  }

  // ==================== queryCategorySubId 測試 ====================

  @Nested
  @DisplayName("queryCategorySubId 查詢子類目 ID 測試")
  class QueryCategorySubIdTest {

    @Test
    @DisplayName("正常情況：應該返回所有子類目 ID")
    void shouldReturnAllSubCategoryIds() {
      // Given
      List<Long> categoryIdList = Collections.singletonList(1L);

      CategoryEntity sub1 = createTestCategoryEntity(2L, "Sub1");
      sub1.setParentId(1L);
      CategoryEntity sub2 = createTestCategoryEntity(3L, "Sub2");
      sub2.setParentId(1L);

      when(categoryCacheManager.querySubCategory(1L)).thenReturn(Arrays.asList(sub1, sub2));
      when(categoryCacheManager.querySubCategory(2L)).thenReturn(Collections.emptyList());
      when(categoryCacheManager.querySubCategory(3L)).thenReturn(Collections.emptyList());

      // When
      List<Long> result = categoryQueryService.queryCategorySubId(categoryIdList);

      // Then
      assertThat(result).contains(2L, 3L);
    }

    @Test
    @DisplayName("邊界情況：空列表應返回空結果")
    void shouldReturnEmptyWhenListEmpty() {
      // When
      List<Long> result = categoryQueryService.queryCategorySubId(Collections.emptyList());

      // Then
      assertThat(result).isEmpty();
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
