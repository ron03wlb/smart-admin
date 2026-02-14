package net.lab1024.sa.api.business.contract;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.api.business.dto.BrandDTO;

/**
 * 品牌服務 API 契約
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
@Tag(name = "品牌服務 API", description = "品牌信息查詢服務")
public interface BrandContract {

  /**
   * 查詢所有品牌（非刪除）
   *
   * @return 品牌列表（非空，可能為空列表）
   */
  @Operation(summary = "查詢所有品牌", description = "返回所有非刪除狀態的品牌")
  List<BrandDTO> listAll();

  /**
   * 根據 ID 查詢品牌
   *
   * @param brandId 品牌 ID
   * @return Option 包裝的品牌對象（不存在返回 Option.none()）
   */
  @Operation(summary = "根據 ID 查詢品牌", description = "使用 Vavr Option 保證類型安全")
  Option<BrandDTO> getById(Long brandId);

  /**
   * 根據品牌名稱關鍵字查詢
   *
   * @param nameKeyword 名稱關鍵字
   * @return 品牌列表
   */
  @Operation(summary = "根據名稱關鍵字查詢品牌", description = "支持模糊匹配，返回匹配的品牌列表")
  List<BrandDTO> queryByNameKeyword(String nameKeyword);
}
