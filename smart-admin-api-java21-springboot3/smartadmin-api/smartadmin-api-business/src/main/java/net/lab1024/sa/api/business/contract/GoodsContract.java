package net.lab1024.sa.api.business.contract;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.api.business.dto.GoodsDTO;

/**
 * 商品服務 API 契約
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 保證類型安全
 *   <li>適配 Feign 遠程調用
 *   <li>高可重複性方法
 *   <li>無副作用的查詢操作
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Tag(name = "商品服務 API", description = "商品信息查詢服務")
public interface GoodsContract {

  /**
   * 根據 ID 查詢商品
   *
   * @param goodsId 商品 ID
   * @return Option 包裝的商品對象
   */
  @Operation(summary = "根據 ID 查詢商品", description = "使用 Vavr Option 保證類型安全")
  Option<GoodsDTO> getById(Long goodsId);

  /**
   * 根據分類 ID 查詢商品列表
   *
   * @param categoryId 分類 ID
   * @return 商品列表（非空，可能為空列表）
   */
  @Operation(summary = "根據分類 ID 查詢商品", description = "返回指定分類下的所有商品")
  List<GoodsDTO> queryByCategoryId(Long categoryId);

  /**
   * 查詢所有上架商品（非刪除且上架）
   *
   * @return 商品列表
   */
  @Operation(summary = "查詢所有上架商品", description = "返回所有非刪除且已上架的商品")
  List<GoodsDTO> listAllOnShelf();
}
