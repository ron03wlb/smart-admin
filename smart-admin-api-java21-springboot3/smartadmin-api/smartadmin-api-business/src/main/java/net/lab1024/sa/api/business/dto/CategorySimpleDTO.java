package net.lab1024.sa.api.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 分類簡化 DTO
 *
 * <p>用於下拉列表、關聯查詢等場景，僅包含核心字段以減少數據傳輸量。
 *
 * <p>適用場景：
 *
 * <ul>
 *   <li>下拉選擇器（Select Component）
 *   <li>關聯對象的 category 字段
 *   <li>避免傳輸完整的 {@link CategoryDTO} 造成性能浪費
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Schema(description = "分類簡化 DTO（下拉列表專用）")
public class CategorySimpleDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "分類 ID")
  private Long categoryId;

  @Schema(description = "分類名稱")
  private String categoryName;

  @Schema(description = "父分類 ID")
  private Long parentId;

  @Schema(description = "分類類型")
  private Integer categoryType;
}
