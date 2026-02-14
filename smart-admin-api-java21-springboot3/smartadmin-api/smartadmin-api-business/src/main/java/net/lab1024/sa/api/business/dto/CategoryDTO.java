package net.lab1024.sa.api.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 分類 DTO（基本信息）
 *
 * <p>用於分類基本信息的跨模塊傳輸。
 *
 * <p>與 {@link CategoryTreeDTO} 的區別：
 *
 * <ul>
 *   <li>CategoryDTO：不包含 children 字段，用於列表查詢
 *   <li>CategoryTreeDTO：包含 children 字段，用於樹結構查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Schema(description = "分類 DTO")
public class CategoryDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "分類 ID")
  private Long categoryId;

  @Schema(description = "分類名稱")
  private String categoryName;

  @Schema(description = "父分類 ID")
  private Long parentId;

  @Schema(description = "分類類型")
  private Integer categoryType;

  @Schema(description = "完整路徑名稱（如：醫考/醫師資格/臨床執業）")
  private String fullName;

  @Schema(description = "排序")
  private Integer sort;

  @Schema(description = "禁用標識")
  private Boolean disabledFlag;

  @Schema(description = "更新時間")
  private OffsetDateTime updateTime;

  @Schema(description = "創建時間")
  private OffsetDateTime createTime;
}
