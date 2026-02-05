package net.lab1024.sa.api.business.contract;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Option;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.api.business.dto.CategoryDTO;
import net.lab1024.sa.api.business.dto.CategorySimpleDTO;
import net.lab1024.sa.api.business.dto.CategoryTreeDTO;

/**
 * 分類服務 API 契約
 *
 * <p>核心功能：
 *
 * <ul>
 *   <li>樹結構查詢（父子關係）
 *   <li>完整路徑名稱查詢（如 "醫考/醫師資格/臨床執業"）
 *   <li>遞歸子級 ID 查詢（含自身）
 *   <li>批量查詢優化（避免 N+1 問題）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Tag(name = "分類服務 API", description = "分類信息查詢服務，支持樹結構和路徑查詢")
public interface CategoryContract {

  // ==================== 基本 CRUD ====================

  /**
   * 根據 ID 查詢分類
   *
   * @param categoryId 分類 ID
   * @return Option 包裝的分類對象
   */
  @Operation(summary = "根據 ID 查詢分類", description = "使用 Vavr Option 保證類型安全")
  Option<CategoryDTO> getById(Long categoryId);

  /**
   * 批量查詢分類（避免 N+1 查詢）
   *
   * @param categoryIds 分類 ID 集合
   * @return Map<分類ID, 分類對象>（保證返回非 null，不存在的 ID 不在 Map 中）
   */
  @Operation(summary = "批量查詢分類", description = "返回 Map 結構避免 N+1 問題，不存在的 ID 不在 Map 中")
  Map<Long, CategoryDTO> queryByIds(Collection<Long> categoryIds);

  /**
   * 查詢所有分類（非刪除）
   *
   * @return 分類列表
   */
  @Operation(summary = "查詢所有分類", description = "返回所有非刪除狀態的分類")
  List<CategoryDTO> listAll();

  // ==================== 樹結構查詢 ====================

  /**
   * 獲取分類樹（按類型）
   *
   * @param categoryType 分類類型
   * @return 分類樹結構（List 的元素是頂級節點，每個節點包含 children）
   */
  @Operation(summary = "獲取分類樹", description = "返回樹結構，每個節點包含遞歸的 children 列表")
  List<CategoryTreeDTO> getTree(Integer categoryType);

  /**
   * 獲取分類簡化樹（下拉列表專用，不含完整字段）
   *
   * @param categoryType 分類類型
   * @return 簡化的分類樹
   */
  @Operation(summary = "獲取分類簡化樹", description = "僅包含核心字段，用於下拉選擇器，減少數據傳輸量")
  List<CategorySimpleDTO> getSimpleTree(Integer categoryType);

  // ==================== 路徑查詢 ====================

  /**
   * 獲取分類的完整路徑名稱
   *
   * <p>示例: categoryId=3 → "醫考/醫師資格/臨床執業"
   *
   * @param categoryId 分類 ID
   * @return Option 包裝的完整路徑名稱（不存在返回 Option.none()）
   */
  @Operation(summary = "獲取分類完整路徑", description = "返回從根節點到當前節點的完整路徑名稱")
  Option<String> getCategoryFullName(Long categoryId);

  /**
   * 批量獲取分類的完整路徑名稱（避免 N+1 查詢）
   *
   * @param categoryIds 分類 ID 集合
   * @return Map<分類ID, 完整路徑名稱>
   */
  @Operation(summary = "批量獲取分類完整路徑", description = "返回 Map 結構避免 N+1 問題")
  Map<Long, String> getCategoryFullNameBatch(Collection<Long> categoryIds);

  // ==================== 子級查詢 ====================

  /**
   * 獲取指定分類的直接子節點
   *
   * @param parentId 父分類 ID
   * @return 子分類列表
   */
  @Operation(summary = "獲取直接子分類", description = "僅返回一級子節點，不遞歸")
  List<CategoryDTO> getChildren(Long parentId);

  /**
   * 獲取指定分類的所有子孫 ID（遞歸查詢，包含自身）
   *
   * <p>示例: categoryId=1 → [1, 2, 3, 4, 5]（1 是自己，2-5 是子孫）
   *
   * @param categoryId 分類 ID
   * @return ID 列表（第一個元素是自身，後續是所有子孫）
   */
  @Operation(summary = "獲取分類及所有子孫 ID", description = "遞歸查詢，返回包含自身的完整 ID 列表")
  List<Long> getSelfAndDescendantIds(Long categoryId);
}
