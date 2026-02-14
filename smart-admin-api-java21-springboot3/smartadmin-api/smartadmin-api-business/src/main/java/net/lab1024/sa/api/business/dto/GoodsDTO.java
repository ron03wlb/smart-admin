package net.lab1024.sa.api.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 商品 DTO
 *
 * <p>用於商品信息的跨模塊傳輸，支持 Feign 遠程調用。
 *
 * <p>設計考量：
 *
 * <ul>
 *   <li>包含 categoryName 冗餘字段，避免 N+1 查詢問題
 *   <li>當需要展示商品列表時，無需額外查詢 Category 信息
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Schema(description = "商品 DTO")
public class GoodsDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "商品 ID")
  private Long goodsId;

  @Schema(description = "分類 ID")
  private Long categoryId;

  @Schema(description = "分類名稱（冗餘字段，避免 N+1 查詢）")
  private String categoryName;

  @Schema(description = "商品名稱")
  private String goodsName;

  @Schema(description = "商品編碼")
  private String goodsCode;

  @Schema(description = "縮略圖")
  private String thumbnail;

  @Schema(description = "商品詳情")
  private String detail;

  @Schema(description = "價格")
  private BigDecimal price;

  @Schema(description = "庫存")
  private Integer stock;

  @Schema(description = "銷售數量")
  private Integer salesCount;

  @Schema(description = "上架標識")
  private Boolean shelfFlag;

  @Schema(description = "刪除標識")
  private Boolean deletedFlag;

  @Schema(description = "更新時間")
  private OffsetDateTime updateTime;

  @Schema(description = "創建時間")
  private OffsetDateTime createTime;
}
