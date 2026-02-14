package net.lab1024.sa.business.adapter;

import io.vavr.control.Option;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.business.contract.CategoryContract;
import net.lab1024.sa.api.business.dto.CategoryDTO;
import net.lab1024.sa.api.business.dto.CategorySimpleDTO;
import net.lab1024.sa.api.business.dto.CategoryTreeDTO;
import net.lab1024.sa.business.category.dao.CategoryDao;
import net.lab1024.sa.business.category.domain.form.CategoryTreeQueryForm;
import net.lab1024.sa.business.category.service.CategoryQueryService;
import net.lab1024.sa.business.category.service.CategoryService;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Component;

/**
 * 分類契約適配器
 *
 * <p>Adapter Pattern: 將 CategoryService/CategoryQueryService 適配到 CategoryContract API 契約。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 替代 null 返回
 *   <li>批量查詢返回 Map 避免 N+1 問題
 *   <li>樹結構查詢使用 CategoryQueryService（緩存優化）
 *   <li>參數驗證拋出 IllegalArgumentException
 *   <li>使用 SmartBeanUtil 進行對象轉換
 *   <li>無副作用的查詢操作
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class CategoryContractAdapter implements CategoryContract {

  private final CategoryService categoryService;
  private final CategoryQueryService categoryQueryService;
  private final CategoryDao categoryDao;

  // ==================== 基本 CRUD ====================

  /**
   * 根據 ID 查詢分類
   *
   * @param categoryId 分類 ID
   * @return Option 包裝的分類對象
   * @throws IllegalArgumentException 如果 categoryId 為 null
   */
  @Override
  public Option<CategoryDTO> getById(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId cannot be null");
    }
    return categoryQueryService
        .queryCategory(categoryId)
        .map(entity -> SmartBeanUtil.copy(entity, CategoryDTO.class));
  }

  /**
   * 批量查詢分類（避免 N+1 查詢）
   *
   * @param categoryIds 分類 ID 集合
   * @return Map<分類ID, 分類對象>（保證返回非 null，不存在的 ID 不在 Map 中）
   * @throws IllegalArgumentException 如果 categoryIds 為 null
   */
  @Override
  public Map<Long, CategoryDTO> queryByIds(Collection<Long> categoryIds) {
    if (categoryIds == null) {
      throw new IllegalArgumentException("categoryIds cannot be null");
    }
    if (categoryIds.isEmpty()) {
      return Map.of();
    }
    return categoryQueryService.queryCategoryList(List.copyOf(categoryIds)).entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> SmartBeanUtil.copy(entry.getValue(), CategoryDTO.class)));
  }

  /**
   * 查詢所有分類（非刪除）
   *
   * @return 分類列表
   */
  @Override
  public List<CategoryDTO> listAll() {
    return categoryDao.selectList(null).stream()
        .filter(entity -> !entity.getDeletedFlag())
        .map(entity -> SmartBeanUtil.copy(entity, CategoryDTO.class))
        .collect(Collectors.toList());
  }

  // ==================== 樹結構查詢 ====================

  /**
   * 獲取分類樹（按類型）
   *
   * @param categoryType 分類類型
   * @return 分類樹結構（List 的元素是頂級節點，每個節點包含 children）
   * @throws IllegalArgumentException 如果 categoryType 為 null
   */
  @Override
  public List<CategoryTreeDTO> getTree(Integer categoryType) {
    if (categoryType == null) {
      throw new IllegalArgumentException("categoryType cannot be null");
    }
    CategoryTreeQueryForm queryForm = new CategoryTreeQueryForm();
    queryForm.setCategoryType(categoryType);
    return categoryService.queryTree(queryForm).getData().stream()
        .map(treeVO -> SmartBeanUtil.copy(treeVO, CategoryTreeDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 獲取分類簡化樹（下拉列表專用，不含完整字段）
   *
   * @param categoryType 分類類型
   * @return 簡化的分類樹
   * @throws IllegalArgumentException 如果 categoryType 為 null
   */
  @Override
  public List<CategorySimpleDTO> getSimpleTree(Integer categoryType) {
    if (categoryType == null) {
      throw new IllegalArgumentException("categoryType cannot be null");
    }
    CategoryTreeQueryForm queryForm = new CategoryTreeQueryForm();
    queryForm.setCategoryType(categoryType);
    return categoryService.queryTree(queryForm).getData().stream()
        .map(treeVO -> SmartBeanUtil.copy(treeVO, CategorySimpleDTO.class))
        .collect(Collectors.toList());
  }

  // ==================== 路徑查詢 ====================

  /**
   * 獲取分類的完整路徑名稱
   *
   * <p>示例: categoryId=3 → "醫考/醫師資格/臨床執業"
   *
   * @param categoryId 分類 ID
   * @return Option 包裝的完整路徑名稱（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 categoryId 為 null
   */
  @Override
  public Option<String> getCategoryFullName(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId cannot be null");
    }
    return Option.of(categoryQueryService.queryFullName(categoryId));
  }

  /**
   * 批量獲取分類的完整路徑名稱（避免 N+1 查詢）
   *
   * @param categoryIds 分類 ID 集合
   * @return Map<分類ID, 完整路徑名稱>
   * @throws IllegalArgumentException 如果 categoryIds 為 null
   */
  @Override
  public Map<Long, String> getCategoryFullNameBatch(Collection<Long> categoryIds) {
    if (categoryIds == null) {
      throw new IllegalArgumentException("categoryIds cannot be null");
    }
    if (categoryIds.isEmpty()) {
      return Map.of();
    }
    return categoryQueryService.queryFullName(List.copyOf(categoryIds));
  }

  // ==================== 子級查詢 ====================

  /**
   * 獲取指定分類的直接子節點
   *
   * @param parentId 父分類 ID
   * @return 子分類列表
   * @throws IllegalArgumentException 如果 parentId 為 null
   */
  @Override
  public List<CategoryDTO> getChildren(Long parentId) {
    if (parentId == null) {
      throw new IllegalArgumentException("parentId cannot be null");
    }
    return categoryDao.queryByParentId(Collections.singletonList(parentId), Boolean.FALSE).stream()
        .map(entity -> SmartBeanUtil.copy(entity, CategoryDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 獲取指定分類的所有子孫 ID（遞歸查詢，包含自身）
   *
   * <p>示例: categoryId=1 → [1, 2, 3, 4, 5]（1 是自己，2-5 是子孫）
   *
   * @param categoryId 分類 ID
   * @return ID 列表（第一個元素是自身，後續是所有子孫）
   * @throws IllegalArgumentException 如果 categoryId 為 null
   */
  @Override
  public List<Long> getSelfAndDescendantIds(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId cannot be null");
    }
    return categoryQueryService.queryCategorySubId(List.of(categoryId));
  }
}
