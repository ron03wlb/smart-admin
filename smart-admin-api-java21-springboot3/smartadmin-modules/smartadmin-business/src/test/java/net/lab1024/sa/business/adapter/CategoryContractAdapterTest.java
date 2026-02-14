package net.lab1024.sa.business.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.api.business.dto.CategoryDTO;
import net.lab1024.sa.api.business.dto.CategorySimpleDTO;
import net.lab1024.sa.api.business.dto.CategoryTreeDTO;
import net.lab1024.sa.business.category.dao.CategoryDao;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.category.domain.form.CategoryTreeQueryForm;
import net.lab1024.sa.business.category.domain.vo.CategoryTreeVO;
import net.lab1024.sa.business.category.service.CategoryQueryService;
import net.lab1024.sa.business.category.service.CategoryService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CategoryContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程（9 個方法）
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到、空集合）
 *   <li>Vavr Option 正確使用
 *   <li>Map 返回類型優化（批量查詢）
 *   <li>樹結構處理
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class CategoryContractAdapterTest {

  @Mock private CategoryService categoryService;

  @Mock private CategoryQueryService categoryQueryService;

  @Mock private CategoryDao categoryDao;

  @InjectMocks private CategoryContractAdapter adapter;

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long categoryId = 1L;
    CategoryEntity entity = new CategoryEntity();
    entity.setCategoryId(categoryId);
    entity.setCategoryName("電子產品");

    when(categoryQueryService.queryCategory(categoryId)).thenReturn(Option.of(entity));

    // When
    Option<CategoryDTO> result = adapter.getById(categoryId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getCategoryId()).isEqualTo(categoryId);
    assertThat(result.get().getCategoryName()).isEqualTo("電子產品");
    verify(categoryQueryService).queryCategory(categoryId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long categoryId = 999L;
    when(categoryQueryService.queryCategory(categoryId)).thenReturn(Option.none());

    // When
    Option<CategoryDTO> result = adapter.getById(categoryId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(categoryQueryService).queryCategory(categoryId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId cannot be null");
  }

  // ==================== queryByIds 測試 ====================

  @Test
  void testQueryByIds_Success() {
    // Given
    List<Long> categoryIds = Arrays.asList(1L, 2L);

    CategoryEntity entity1 = new CategoryEntity();
    entity1.setCategoryId(1L);
    entity1.setCategoryName("電子產品");

    CategoryEntity entity2 = new CategoryEntity();
    entity2.setCategoryId(2L);
    entity2.setCategoryName("家電");

    Map<Long, CategoryEntity> entityMap = new HashMap<>();
    entityMap.put(1L, entity1);
    entityMap.put(2L, entity2);

    when(categoryQueryService.queryCategoryList(categoryIds)).thenReturn(entityMap);

    // When
    Map<Long, CategoryDTO> result = adapter.queryByIds(categoryIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(1L).getCategoryName()).isEqualTo("電子產品");
    assertThat(result.get(2L).getCategoryName()).isEqualTo("家電");
    verify(categoryQueryService).queryCategoryList(categoryIds);
  }

  @Test
  void testQueryByIds_EmptyList() {
    // Given
    List<Long> categoryIds = Collections.emptyList();

    // When
    Map<Long, CategoryDTO> result = adapter.queryByIds(categoryIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testQueryByIds_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByIds(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryIds cannot be null");
  }

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    CategoryEntity entity1 = new CategoryEntity();
    entity1.setCategoryId(1L);
    entity1.setCategoryName("電子產品");
    entity1.setDeletedFlag(Boolean.FALSE);

    CategoryEntity entity2 = new CategoryEntity();
    entity2.setCategoryId(2L);
    entity2.setCategoryName("家電");
    entity2.setDeletedFlag(Boolean.FALSE);

    List<CategoryEntity> entityList = Arrays.asList(entity1, entity2);
    when(categoryDao.selectList(null)).thenReturn(entityList);

    // When
    List<CategoryDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCategoryName()).isEqualTo("電子產品");
    assertThat(result.get(1).getCategoryName()).isEqualTo("家電");
    verify(categoryDao).selectList(null);
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    when(categoryDao.selectList(null)).thenReturn(Collections.emptyList());

    // When
    List<CategoryDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(categoryDao).selectList(null);
  }

  // ==================== getTree 測試 ====================

  @Test
  void testGetTree_Success() {
    // Given
    Integer categoryType = 1;

    CategoryTreeVO treeVO1 = new CategoryTreeVO();
    treeVO1.setCategoryId(1L);
    treeVO1.setCategoryName("電子產品");

    CategoryTreeVO treeVO2 = new CategoryTreeVO();
    treeVO2.setCategoryId(2L);
    treeVO2.setCategoryName("家電");

    List<CategoryTreeVO> treeVOList = Arrays.asList(treeVO1, treeVO2);
    ResponseDTO<List<CategoryTreeVO>> response = ResponseDTO.ok(treeVOList);

    when(categoryService.queryTree(any(CategoryTreeQueryForm.class))).thenReturn(response);

    // When
    List<CategoryTreeDTO> result = adapter.getTree(categoryType);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCategoryName()).isEqualTo("電子產品");
    assertThat(result.get(1).getCategoryName()).isEqualTo("家電");
    verify(categoryService).queryTree(any(CategoryTreeQueryForm.class));
  }

  @Test
  void testGetTree_EmptyResult() {
    // Given
    Integer categoryType = 999;
    ResponseDTO<List<CategoryTreeVO>> response = ResponseDTO.ok(Collections.emptyList());

    when(categoryService.queryTree(any(CategoryTreeQueryForm.class))).thenReturn(response);

    // When
    List<CategoryTreeDTO> result = adapter.getTree(categoryType);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(categoryService).queryTree(any(CategoryTreeQueryForm.class));
  }

  @Test
  void testGetTree_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getTree(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryType cannot be null");
  }

  // ==================== getSimpleTree 測試 ====================

  @Test
  void testGetSimpleTree_Success() {
    // Given
    Integer categoryType = 1;

    CategoryTreeVO treeVO1 = new CategoryTreeVO();
    treeVO1.setCategoryId(1L);
    treeVO1.setCategoryName("電子產品");

    List<CategoryTreeVO> treeVOList = Collections.singletonList(treeVO1);
    ResponseDTO<List<CategoryTreeVO>> response = ResponseDTO.ok(treeVOList);

    when(categoryService.queryTree(any(CategoryTreeQueryForm.class))).thenReturn(response);

    // When
    List<CategorySimpleDTO> result = adapter.getSimpleTree(categoryType);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCategoryName()).isEqualTo("電子產品");
    verify(categoryService).queryTree(any(CategoryTreeQueryForm.class));
  }

  @Test
  void testGetSimpleTree_EmptyResult() {
    // Given
    Integer categoryType = 999;
    ResponseDTO<List<CategoryTreeVO>> response = ResponseDTO.ok(Collections.emptyList());

    when(categoryService.queryTree(any(CategoryTreeQueryForm.class))).thenReturn(response);

    // When
    List<CategorySimpleDTO> result = adapter.getSimpleTree(categoryType);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(categoryService).queryTree(any(CategoryTreeQueryForm.class));
  }

  @Test
  void testGetSimpleTree_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getSimpleTree(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryType cannot be null");
  }

  // ==================== getCategoryFullName 測試 ====================

  @Test
  void testGetCategoryFullName_Found() {
    // Given
    Long categoryId = 1L;
    String fullName = "電子產品/手機/智能手機";

    when(categoryQueryService.queryFullName(categoryId)).thenReturn(fullName);

    // When
    Option<String> result = adapter.getCategoryFullName(categoryId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get()).isEqualTo(fullName);
    verify(categoryQueryService).queryFullName(categoryId);
  }

  @Test
  void testGetCategoryFullName_NotFound() {
    // Given
    Long categoryId = 999L;
    when(categoryQueryService.queryFullName(categoryId)).thenReturn(null);

    // When
    Option<String> result = adapter.getCategoryFullName(categoryId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(categoryQueryService).queryFullName(categoryId);
  }

  @Test
  void testGetCategoryFullName_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getCategoryFullName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId cannot be null");
  }

  // ==================== getCategoryFullNameBatch 測試 ====================

  @Test
  void testGetCategoryFullNameBatch_Success() {
    // Given
    List<Long> categoryIds = Arrays.asList(1L, 2L);

    Map<Long, String> fullNameMap = new HashMap<>();
    fullNameMap.put(1L, "電子產品/手機");
    fullNameMap.put(2L, "家電/廚房電器");

    when(categoryQueryService.queryFullName(categoryIds)).thenReturn(fullNameMap);

    // When
    Map<Long, String> result = adapter.getCategoryFullNameBatch(categoryIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(1L)).isEqualTo("電子產品/手機");
    assertThat(result.get(2L)).isEqualTo("家電/廚房電器");
    verify(categoryQueryService).queryFullName(categoryIds);
  }

  @Test
  void testGetCategoryFullNameBatch_EmptyList() {
    // Given
    List<Long> categoryIds = Collections.emptyList();

    // When
    Map<Long, String> result = adapter.getCategoryFullNameBatch(categoryIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testGetCategoryFullNameBatch_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getCategoryFullNameBatch(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryIds cannot be null");
  }

  // ==================== getChildren 測試 ====================

  @Test
  void testGetChildren_Success() {
    // Given
    Long parentId = 1L;

    CategoryEntity entity1 = new CategoryEntity();
    entity1.setCategoryId(2L);
    entity1.setCategoryName("手機");
    entity1.setParentId(parentId);

    CategoryEntity entity2 = new CategoryEntity();
    entity2.setCategoryId(3L);
    entity2.setCategoryName("平板");
    entity2.setParentId(parentId);

    List<CategoryEntity> entityList = Arrays.asList(entity1, entity2);
    when(categoryDao.queryByParentId(anyList(), eq(Boolean.FALSE))).thenReturn(entityList);

    // When
    List<CategoryDTO> result = adapter.getChildren(parentId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCategoryName()).isEqualTo("手機");
    assertThat(result.get(1).getCategoryName()).isEqualTo("平板");
    verify(categoryDao).queryByParentId(anyList(), eq(Boolean.FALSE));
  }

  @Test
  void testGetChildren_EmptyResult() {
    // Given
    Long parentId = 999L;
    when(categoryDao.queryByParentId(anyList(), eq(Boolean.FALSE)))
        .thenReturn(Collections.emptyList());

    // When
    List<CategoryDTO> result = adapter.getChildren(parentId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(categoryDao).queryByParentId(anyList(), eq(Boolean.FALSE));
  }

  @Test
  void testGetChildren_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getChildren(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("parentId cannot be null");
  }

  // ==================== getSelfAndDescendantIds 測試 ====================

  @Test
  void testGetSelfAndDescendantIds_Success() {
    // Given
    Long categoryId = 1L;
    List<Long> descendantIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);

    when(categoryQueryService.queryCategorySubId(anyList())).thenReturn(descendantIds);

    // When
    List<Long> result = adapter.getSelfAndDescendantIds(categoryId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(5);
    assertThat(result.get(0)).isEqualTo(1L); // 自身
    assertThat(result).containsExactlyElementsOf(descendantIds);
    verify(categoryQueryService).queryCategorySubId(anyList());
  }

  @Test
  void testGetSelfAndDescendantIds_SingleNode() {
    // Given
    Long categoryId = 10L;
    List<Long> descendantIds = Collections.singletonList(10L); // 只有自己

    when(categoryQueryService.queryCategorySubId(anyList())).thenReturn(descendantIds);

    // When
    List<Long> result = adapter.getSelfAndDescendantIds(categoryId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(10L);
    verify(categoryQueryService).queryCategorySubId(anyList());
  }

  @Test
  void testGetSelfAndDescendantIds_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getSelfAndDescendantIds(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId cannot be null");
  }
}
