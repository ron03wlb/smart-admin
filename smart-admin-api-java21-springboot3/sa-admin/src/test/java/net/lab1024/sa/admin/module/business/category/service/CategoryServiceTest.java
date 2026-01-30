package net.lab1024.sa.admin.module.business.category.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.admin.module.business.category.CategoryTestFixture;
import net.lab1024.sa.admin.module.business.category.constant.CategoryTypeEnum;
import net.lab1024.sa.admin.module.business.category.dao.CategoryDao;
import net.lab1024.sa.admin.module.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.admin.module.business.category.domain.form.CategoryAddForm;
import net.lab1024.sa.admin.module.business.category.domain.form.CategoryTreeQueryForm;
import net.lab1024.sa.admin.module.business.category.domain.form.CategoryUpdateForm;
import net.lab1024.sa.admin.module.business.category.domain.vo.CategoryTreeVO;
import net.lab1024.sa.admin.module.business.category.domain.vo.CategoryVO;
import net.lab1024.sa.admin.module.business.category.manager.CategoryCacheManager;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CategoryService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>类目添加（父类校验、名称重复校验、类型一致性）
 *   <li>类目更新（不可变更父类和类型）
 *   <li>类目查询（详情、树形结构）
 *   <li>类目删除（子类目检查）
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 单元测试")
class CategoryServiceTest {

  @Mock private CategoryDao categoryDao;

  @Mock private CategoryCacheManager categoryCacheManager;

  @InjectMocks private CategoryService categoryService;

  @BeforeEach
  void setUp() {
    CategoryTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("add() - 添加类目")
  class AddTests {

    @Test
    @DisplayName("正常添加根类目 - 应返回成功")
    void add_ValidRootCategory_ShouldReturnSuccess() {
      // Arrange
      CategoryAddForm form =
          CategoryTestFixture.createAddForm(null, CategoryTypeEnum.GOODS.getValue());

      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(null);
      when(categoryDao.insert(any(CategoryEntity.class))).thenReturn(1);
      doNothing().when(categoryCacheManager).removeCache();

      // Act
      ResponseDTO<String> response = categoryService.add(form);

      // Assert
      assertTrue(response.getOk());
      verify(categoryDao, times(1)).insert(any(CategoryEntity.class));
      verify(categoryCacheManager, times(1)).removeCache();
    }

    @Test
    @DisplayName("正常添加子类目 - 应返回成功")
    void add_ValidChildCategory_ShouldReturnSuccess() {
      // Arrange
      Long parentId = 1L;
      CategoryAddForm form =
          CategoryTestFixture.createAddForm(parentId, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(parentId, 0L, CategoryTypeEnum.GOODS.getValue());

      when(categoryCacheManager.queryCategory(parentId)).thenReturn(parentEntity);
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(null);
      when(categoryDao.insert(any(CategoryEntity.class))).thenReturn(1);
      doNothing().when(categoryCacheManager).removeCache();

      // Act
      ResponseDTO<String> response = categoryService.add(form);

      // Assert
      assertTrue(response.getOk());
      verify(categoryDao, times(1)).insert(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("父类目不存在 - 应返回错误")
    void add_ParentNotFound_ShouldReturnError() {
      // Arrange
      Long parentId = 999L;
      CategoryAddForm form =
          CategoryTestFixture.createAddForm(parentId, CategoryTypeEnum.GOODS.getValue());

      when(categoryCacheManager.queryCategory(parentId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = categoryService.add(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("父级类目不存在"));
      verify(categoryDao, never()).insert(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("父类目已删除 - 应返回错误")
    void add_ParentDeleted_ShouldReturnError() {
      // Arrange
      Long parentId = 1L;
      CategoryAddForm form =
          CategoryTestFixture.createAddForm(parentId, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(parentId, 0L, CategoryTypeEnum.GOODS.getValue());
      parentEntity.setDeletedFlag(true);

      when(categoryCacheManager.queryCategory(parentId)).thenReturn(parentEntity);

      // Act
      ResponseDTO<String> response = categoryService.add(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("父级类目不存在"));
    }

    @Test
    @DisplayName("与父级类目类型不一致 - 应返回错误")
    void add_CategoryTypeNotMatch_ShouldReturnError() {
      // Arrange
      Long parentId = 1L;
      CategoryAddForm form =
          CategoryTestFixture.createAddForm(parentId, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(
              parentId, 0L, CategoryTypeEnum.CUSTOM.getValue()); // Different type

      when(categoryCacheManager.queryCategory(parentId)).thenReturn(parentEntity);

      // Act
      ResponseDTO<String> response = categoryService.add(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("与父级类目类型不一致"));
    }

    @Test
    @DisplayName("同级下类目名称重复 - 应返回错误")
    void add_DuplicateName_ShouldReturnError() {
      // Arrange
      Long parentId = 1L;
      CategoryAddForm form =
          CategoryTestFixture.createAddForm(parentId, CategoryTypeEnum.GOODS.getValue());
      form.setCategoryName("重复类目");

      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(parentId, 0L, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity existingCategory =
          CategoryTestFixture.createEntity(2L, parentId, CategoryTypeEnum.GOODS.getValue());
      existingCategory.setCategoryName("重复类目");

      when(categoryCacheManager.queryCategory(parentId)).thenReturn(parentEntity);
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(existingCategory);

      // Act
      ResponseDTO<String> response = categoryService.add(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("同级下已存在相同类目"));
      verify(categoryDao, never()).insert(any(CategoryEntity.class));
    }
  }

  @Nested
  @DisplayName("update() - 更新类目")
  class UpdateTests {

    @Test
    @DisplayName("正常更新类目 - 应返回成功")
    void update_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long categoryId = 100L;
      Long parentId = 1L;
      CategoryUpdateForm form = CategoryTestFixture.createUpdateForm(categoryId);

      CategoryEntity existingEntity =
          CategoryTestFixture.createEntity(categoryId, parentId, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(
              parentId, 0L, CategoryTypeEnum.GOODS.getValue()); // Parent category

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(existingEntity);
      when(categoryCacheManager.queryCategory(parentId))
          .thenReturn(parentEntity); // Mock parent query
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(null);
      when(categoryDao.updateById(any(CategoryEntity.class))).thenReturn(1);
      doNothing().when(categoryCacheManager).removeCache();

      // Act
      ResponseDTO<String> response = categoryService.update(form);

      // Assert
      assertTrue(response.getOk());
      verify(categoryDao, times(1)).updateById(any(CategoryEntity.class));
      verify(categoryCacheManager, times(1)).removeCache();
    }

    @Test
    @DisplayName("类目不存在 - 应返回错误")
    void update_CategoryNotFound_ShouldReturnError() {
      // Arrange
      Long categoryId = 999L;
      CategoryUpdateForm form = CategoryTestFixture.createUpdateForm(categoryId);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = categoryService.update(form);

      // Assert
      assertFalse(response.getOk());
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("同级下类目名称重复 - 应返回错误")
    void update_DuplicateName_ShouldReturnError() {
      // Arrange
      Long categoryId = 100L;
      Long parentId = 1L;
      CategoryUpdateForm form = CategoryTestFixture.createUpdateForm(categoryId);
      form.setCategoryName("重复类目");

      CategoryEntity existingEntity =
          CategoryTestFixture.createEntity(categoryId, parentId, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(
              parentId, 0L, CategoryTypeEnum.GOODS.getValue()); // Parent category
      CategoryEntity duplicateEntity =
          CategoryTestFixture.createEntity(200L, parentId, CategoryTypeEnum.GOODS.getValue());
      duplicateEntity.setCategoryName("重复类目");

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(existingEntity);
      when(categoryCacheManager.queryCategory(parentId))
          .thenReturn(parentEntity); // Mock parent query
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(duplicateEntity);

      // Act
      ResponseDTO<String> response = categoryService.update(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("同级下已存在相同类目"));
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("更新为相同名称 - 应返回成功")
    void update_SameName_ShouldReturnSuccess() {
      // Arrange
      Long categoryId = 100L;
      Long parentId = 1L;
      CategoryUpdateForm form = CategoryTestFixture.createUpdateForm(categoryId);
      form.setCategoryName("相同类目");

      CategoryEntity existingEntity =
          CategoryTestFixture.createEntity(categoryId, parentId, CategoryTypeEnum.GOODS.getValue());
      CategoryEntity parentEntity =
          CategoryTestFixture.createEntity(
              parentId, 0L, CategoryTypeEnum.GOODS.getValue()); // Parent category
      CategoryEntity sameEntity =
          CategoryTestFixture.createEntity(categoryId, parentId, CategoryTypeEnum.GOODS.getValue());
      sameEntity.setCategoryName("相同类目");

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(existingEntity);
      when(categoryCacheManager.queryCategory(parentId))
          .thenReturn(parentEntity); // Mock parent query
      when(categoryDao.selectOne(any(CategoryEntity.class))).thenReturn(sameEntity);
      when(categoryDao.updateById(any(CategoryEntity.class))).thenReturn(1);
      doNothing().when(categoryCacheManager).removeCache();

      // Act
      ResponseDTO<String> response = categoryService.update(form);

      // Assert
      assertTrue(response.getOk());
      verify(categoryDao, times(1)).updateById(any(CategoryEntity.class));
    }
  }

  @Nested
  @DisplayName("queryDetail() - 查询类目详情")
  class QueryDetailTests {

    @Test
    @DisplayName("正常查询详情 - 应返回类目详情")
    void queryDetail_ValidId_ShouldReturnDetail() {
      // Arrange
      Long categoryId = 100L;
      CategoryEntity entity =
          CategoryTestFixture.createEntity(categoryId, 1L, CategoryTypeEnum.GOODS.getValue());

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // Act
      ResponseDTO<CategoryVO> response = categoryService.queryDetail(categoryId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(entity.getCategoryName(), response.getData().getCategoryName());
    }

    @Test
    @DisplayName("类目不存在 - 应返回错误")
    void queryDetail_CategoryNotFound_ShouldReturnError() {
      // Arrange
      Long categoryId = 999L;

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(null);

      // Act
      ResponseDTO<CategoryVO> response = categoryService.queryDetail(categoryId);

      // Assert
      assertFalse(response.getOk());
    }

    @Test
    @DisplayName("类目已删除 - 应返回错误")
    void queryDetail_CategoryDeleted_ShouldReturnError() {
      // Arrange
      Long categoryId = 100L;
      CategoryEntity entity =
          CategoryTestFixture.createEntity(categoryId, 1L, CategoryTypeEnum.GOODS.getValue());
      entity.setDeletedFlag(true);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // Act
      ResponseDTO<CategoryVO> response = categoryService.queryDetail(categoryId);

      // Assert
      assertFalse(response.getOk());
    }
  }

  @Nested
  @DisplayName("queryTree() - 查询类目树")
  class QueryTreeTests {

    @Test
    @DisplayName("正常查询类目树 - 应返回树结构")
    void queryTree_ValidForm_ShouldReturnTree() {
      // Arrange
      Long parentId = 1L;
      CategoryTreeQueryForm form =
          CategoryTestFixture.createTreeQueryForm(parentId, CategoryTypeEnum.GOODS.getValue());
      List<CategoryTreeVO> treeList = CategoryTestFixture.createTreeVOList(3, parentId);

      when(categoryCacheManager.queryCategoryTree(parentId, CategoryTypeEnum.GOODS.getValue()))
          .thenReturn(treeList);

      // Act
      ResponseDTO<List<CategoryTreeVO>> response = categoryService.queryTree(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(3, response.getData().size());
    }

    @Test
    @DisplayName("查询根类目树 - 应返回根类目树")
    void queryTree_RootCategory_ShouldReturnRootTree() {
      // Arrange
      CategoryTreeQueryForm form =
          CategoryTestFixture.createTreeQueryForm(null, CategoryTypeEnum.GOODS.getValue());
      List<CategoryTreeVO> treeList = CategoryTestFixture.createTreeVOList(5, 0L);

      when(categoryCacheManager.queryCategoryTree(0L, CategoryTypeEnum.GOODS.getValue()))
          .thenReturn(treeList);

      // Act
      ResponseDTO<List<CategoryTreeVO>> response = categoryService.queryTree(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(5, response.getData().size());
    }

    @Test
    @DisplayName("parentId为空且categoryType为空 - 应返回错误")
    void queryTree_MissingCategoryType_ShouldReturnError() {
      // Arrange
      CategoryTreeQueryForm form = CategoryTestFixture.createTreeQueryForm(null, null);

      // Act
      ResponseDTO<List<CategoryTreeVO>> response = categoryService.queryTree(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("类目类型不能为空"));
    }

    @Test
    @DisplayName("空结果查询 - 应返回空列表")
    void queryTree_NoResults_ShouldReturnEmptyList() {
      // Arrange
      Long parentId = 1L;
      CategoryTreeQueryForm form =
          CategoryTestFixture.createTreeQueryForm(parentId, CategoryTypeEnum.GOODS.getValue());
      List<CategoryTreeVO> emptyList = Collections.emptyList();

      when(categoryCacheManager.queryCategoryTree(parentId, CategoryTypeEnum.GOODS.getValue()))
          .thenReturn(emptyList);

      // Act
      ResponseDTO<List<CategoryTreeVO>> response = categoryService.queryTree(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().isEmpty());
    }
  }

  @Nested
  @DisplayName("delete() - 删除类目")
  class DeleteTests {

    @Test
    @DisplayName("正常删除类目 - 应返回成功")
    void delete_ValidId_ShouldReturnSuccess() {
      // Arrange
      Long categoryId = 100L;
      CategoryEntity entity =
          CategoryTestFixture.createEntity(categoryId, 1L, CategoryTypeEnum.GOODS.getValue());

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);
      when(categoryCacheManager.querySubCategory(categoryId)).thenReturn(Collections.emptyList());
      when(categoryDao.updateById(any(CategoryEntity.class))).thenReturn(1);
      doNothing().when(categoryCacheManager).removeCache();

      // Act
      ResponseDTO<String> response = categoryService.delete(categoryId);

      // Assert
      assertTrue(response.getOk());
      verify(categoryDao, times(1)).updateById(any(CategoryEntity.class));
      verify(categoryCacheManager, times(1)).removeCache();
    }

    @Test
    @DisplayName("类目不存在 - 应返回错误")
    void delete_CategoryNotFound_ShouldReturnError() {
      // Arrange
      Long categoryId = 999L;

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = categoryService.delete(categoryId);

      // Assert
      assertFalse(response.getOk());
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("类目已删除 - 应返回错误")
    void delete_CategoryDeleted_ShouldReturnError() {
      // Arrange
      Long categoryId = 100L;
      CategoryEntity entity =
          CategoryTestFixture.createEntity(categoryId, 1L, CategoryTypeEnum.GOODS.getValue());
      entity.setDeletedFlag(true);

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);

      // Act
      ResponseDTO<String> response = categoryService.delete(categoryId);

      // Assert
      assertFalse(response.getOk());
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("存在未删除的子类目 - 应返回错误")
    void delete_HasActiveSubCategory_ShouldReturnError() {
      // Arrange
      Long categoryId = 100L;
      CategoryEntity entity =
          CategoryTestFixture.createEntity(categoryId, 1L, CategoryTypeEnum.GOODS.getValue());

      List<CategoryEntity> subList =
          CategoryTestFixture.createEntityList(2, categoryId, CategoryTypeEnum.GOODS.getValue());

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);
      when(categoryCacheManager.querySubCategory(categoryId)).thenReturn(subList);

      // Act
      ResponseDTO<String> response = categoryService.delete(categoryId);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("请先删除子级类目"));
      verify(categoryDao, never()).updateById(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("子类目全部已删除 - 应允许删除")
    void delete_AllSubCategoriesDeleted_ShouldAllowDelete() {
      // Arrange
      Long categoryId = 100L;
      CategoryEntity entity =
          CategoryTestFixture.createEntity(categoryId, 1L, CategoryTypeEnum.GOODS.getValue());

      List<CategoryEntity> subList =
          CategoryTestFixture.createEntityList(2, categoryId, CategoryTypeEnum.GOODS.getValue());
      subList.forEach(sub -> sub.setDeletedFlag(true)); // All deleted

      when(categoryCacheManager.queryCategory(categoryId)).thenReturn(entity);
      when(categoryCacheManager.querySubCategory(categoryId)).thenReturn(subList);
      when(categoryDao.updateById(any(CategoryEntity.class))).thenReturn(1);
      doNothing().when(categoryCacheManager).removeCache();

      // Act
      ResponseDTO<String> response = categoryService.delete(categoryId);

      // Assert
      assertTrue(response.getOk());
      verify(categoryDao, times(1)).updateById(any(CategoryEntity.class));
    }
  }
}
