package net.lab1024.sa.business.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.business.category.dao.CategoryDao;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.category.domain.form.CategoryAddForm;
import net.lab1024.sa.business.category.domain.form.CategoryTreeQueryForm;
import net.lab1024.sa.business.category.domain.form.CategoryUpdateForm;
import net.lab1024.sa.business.category.domain.vo.CategoryTreeVO;
import net.lab1024.sa.business.category.domain.vo.CategoryVO;
import net.lab1024.sa.business.category.manager.CategoryCacheManager;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CategoryService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>類目 CRUD 操作
 *   <li>父類目校驗邏輯
 *   <li>同名類目校驗
 *   <li>刪除時子類目檢查
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 單元測試")
class CategoryServiceTest {

  @Mock private CategoryDao categoryDao;

  @Mock private CategoryCacheManager categoryCacheManager;

  @InjectMocks private CategoryService categoryService;

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增類目測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增類目")
    void shouldAddCategorySuccess() {
      // Given
      CategoryAddForm addForm = createTestAddForm("TestCategory", 1);
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(null);

      // When
      ResponseDTO<String> result = categoryService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(categoryDao).insert(any(CategoryEntity.class));
      verify(categoryCacheManager).removeCache();
    }

    @Test
    @DisplayName("異常情況：同級下存在相同名稱時應返回錯誤")
    void shouldReturnErrorWhenNameExists() {
      // Given
      CategoryAddForm addForm = createTestAddForm("ExistingCategory", 1);
      CategoryEntity existingEntity = createTestCategoryEntity(99L, "ExistingCategory");
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(existingEntity);

      // When
      ResponseDTO<String> result = categoryService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("同级下已存在相同类目");
      verify(categoryDao, never()).insert(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("正常情況：有父類目時應驗證父類目存在")
    void shouldValidateParentExists() {
      // Given
      CategoryAddForm addForm = createTestAddForm("ChildCategory", 1);
      addForm.setParentId(100L);

      CategoryEntity parentEntity = createTestCategoryEntity(100L, "ParentCategory");
      parentEntity.setCategoryType(1);
      parentEntity.setDeletedFlag(false);

      when(categoryCacheManager.queryCategory(100L)).thenReturn(parentEntity);
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(null);

      // When
      ResponseDTO<String> result = categoryService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(categoryDao).insert(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("異常情況：父類目不存在時應返回錯誤")
    void shouldReturnErrorWhenParentNotExists() {
      // Given
      CategoryAddForm addForm = createTestAddForm("ChildCategory", 1);
      addForm.setParentId(999L);

      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = categoryService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("父级类目不存在");
      verify(categoryDao, never()).insert(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("異常情況：父類目類型不一致時應返回錯誤")
    void shouldReturnErrorWhenParentTypeNotMatch() {
      // Given
      CategoryAddForm addForm = createTestAddForm("ChildCategory", 1);
      addForm.setParentId(100L);

      CategoryEntity parentEntity = createTestCategoryEntity(100L, "ParentCategory");
      parentEntity.setCategoryType(2); // 類型不一致
      parentEntity.setDeletedFlag(false);

      when(categoryCacheManager.queryCategory(100L)).thenReturn(parentEntity);

      // When
      ResponseDTO<String> result = categoryService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("与父级类目类型不一致");
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新類目測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新類目")
    void shouldUpdateCategorySuccess() {
      // Given
      CategoryUpdateForm updateForm = createTestUpdateForm(1L, "UpdatedCategory");

      CategoryEntity existingEntity = createTestCategoryEntity(1L, "OriginalCategory");
      existingEntity.setDeletedFlag(false);
      existingEntity.setCategoryType(1);
      existingEntity.setParentId(0L);

      when(categoryCacheManager.queryCategory(1L)).thenReturn(existingEntity);
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(null);

      // When
      ResponseDTO<String> result = categoryService.update(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(categoryDao).updateById(any(CategoryEntity.class));
      verify(categoryCacheManager).removeCache();
    }

    @Test
    @DisplayName("異常情況：類目不存在時應返回錯誤")
    void shouldReturnErrorWhenCategoryNotFound() {
      // Given
      CategoryUpdateForm updateForm = createTestUpdateForm(999L, "UpdatedCategory");
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = categoryService.update(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("異常情況：已刪除類目應返回錯誤")
    void shouldReturnErrorWhenCategoryDeleted() {
      // Given
      CategoryUpdateForm updateForm = createTestUpdateForm(1L, "UpdatedCategory");

      CategoryEntity deletedEntity = createTestCategoryEntity(1L, "DeletedCategory");
      deletedEntity.setDeletedFlag(true);

      when(categoryCacheManager.queryCategory(1L)).thenReturn(deletedEntity);

      // When
      ResponseDTO<String> result = categoryService.update(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }
  }

  // ==================== queryDetail 測試 ====================

  @Nested
  @DisplayName("queryDetail 查詢詳情測試")
  class QueryDetailTest {

    @Test
    @DisplayName("正常情況：應該返回類目詳情")
    void shouldReturnCategoryDetail() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "TestCategory");
      entity.setDeletedFlag(false);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // When
      ResponseDTO<CategoryVO> result = categoryService.queryDetail(categoryId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getCategoryName()).isEqualTo("TestCategory");
    }

    @Test
    @DisplayName("異常情況：類目不存在應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      ResponseDTO<CategoryVO> result = categoryService.queryDetail(999L);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== queryTree 測試 ====================

  @Nested
  @DisplayName("queryTree 查詢樹測試")
  class QueryTreeTest {

    @Test
    @DisplayName("正常情況：應該返回類目樹")
    void shouldReturnCategoryTree() {
      // Given
      CategoryTreeQueryForm queryForm = new CategoryTreeQueryForm();
      queryForm.setParentId(0L);
      queryForm.setCategoryType(1);

      CategoryTreeVO treeVO = new CategoryTreeVO();
      treeVO.setCategoryId(1L);
      treeVO.setCategoryName("Root");

      when(categoryCacheManager.queryCategoryTree(0L, 1))
          .thenReturn(Collections.singletonList(treeVO));

      // When
      ResponseDTO<List<CategoryTreeVO>> result = categoryService.queryTree(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }

    @Test
    @DisplayName("異常情況：無父類目且無類目類型時應返回錯誤")
    void shouldReturnErrorWhenNoParentAndNoType() {
      // Given
      CategoryTreeQueryForm queryForm = new CategoryTreeQueryForm();
      queryForm.setParentId(null);
      queryForm.setCategoryType(null);

      // When
      ResponseDTO<List<CategoryTreeVO>> result = categoryService.queryTree(queryForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类目类型不能为空");
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除類目測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除類目")
    void shouldDeleteCategorySuccess() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "ToDelete");
      entity.setDeletedFlag(false);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);
      when(categoryCacheManager.querySubCategory(categoryId)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<String> result = categoryService.delete(categoryId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(categoryDao).updateById(any(CategoryEntity.class));
      verify(categoryCacheManager).removeCache();
    }

    @Test
    @DisplayName("異常情況：有未刪除子類目時應返回錯誤")
    void shouldReturnErrorWhenHasSubCategory() {
      // Given
      Long categoryId = 1L;
      CategoryEntity entity = createTestCategoryEntity(categoryId, "Parent");
      entity.setDeletedFlag(false);

      CategoryEntity subEntity = createTestCategoryEntity(2L, "Child");
      subEntity.setDeletedFlag(false);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);
      when(categoryCacheManager.querySubCategory(categoryId))
          .thenReturn(Collections.singletonList(subEntity));

      // When
      ResponseDTO<String> result = categoryService.delete(categoryId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("请先删除子级类目");
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("異常情況：類目不存在時應返回錯誤")
    void shouldReturnErrorWhenCategoryNotFound() {
      // Given
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = categoryService.delete(999L);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }
  }

  // ==================== Helper Methods ====================

  private CategoryAddForm createTestAddForm(String name, Integer type) {
    CategoryAddForm form = new CategoryAddForm();
    form.setCategoryName(name);
    form.setCategoryType(type);
    form.setParentId(0L);
    return form;
  }

  private CategoryUpdateForm createTestUpdateForm(Long id, String name) {
    CategoryUpdateForm form = new CategoryUpdateForm();
    form.setCategoryId(id);
    form.setCategoryName(name);
    return form;
  }

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
