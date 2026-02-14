package net.lab1024.sa.api.oa.contract;

import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.api.oa.dto.BankDTO;

/**
 * 銀行服務 API 契約
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 保證類型安全
 *   <li>適配 Feign 遠程調用
 *   <li>無副作用的查詢操作
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface BankContract {

  /**
   * 根據企業 ID 查詢銀行列表
   *
   * @param enterpriseId 企業 ID
   * @return 銀行列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 enterpriseId 為 null
   */
  List<BankDTO> queryByEnterpriseId(Long enterpriseId);

  /**
   * 根據 ID 查詢銀行詳情
   *
   * @param bankId 銀行 ID
   * @return Option 包裝的銀行對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 bankId 為 null
   */
  Option<BankDTO> getById(Long bankId);
}
