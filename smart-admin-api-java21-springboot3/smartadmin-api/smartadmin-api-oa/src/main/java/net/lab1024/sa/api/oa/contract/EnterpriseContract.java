package net.lab1024.sa.api.oa.contract;

import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.api.oa.dto.EnterpriseDTO;
import net.lab1024.sa.api.oa.dto.EnterpriseEmployeeDTO;
import net.lab1024.sa.api.oa.dto.EnterpriseSimpleDTO;

/**
 * 企業服務 API 契約
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
public interface EnterpriseContract {

  /**
   * 根據 ID 查詢企業詳情
   *
   * @param enterpriseId 企業 ID
   * @return Option 包裝的企業對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 enterpriseId 為 null
   */
  Option<EnterpriseDTO> getById(Long enterpriseId);

  /**
   * 根據類型查詢企業列表
   *
   * @param type 企業類型
   * @return 企業列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 type 為 null
   */
  List<EnterpriseSimpleDTO> queryByType(Integer type);

  /**
   * 批量查詢企業員工關聯
   *
   * @param enterpriseIds 企業 ID 列表
   * @return 企業員工關聯列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 enterpriseIds 為 null
   */
  List<EnterpriseEmployeeDTO> queryEmployeesByEnterpriseIds(List<Long> enterpriseIds);

  /**
   * 查詢所有企業（非刪除）
   *
   * @return 企業列表（非空，可能為空列表）
   */
  List<EnterpriseSimpleDTO> listAll();
}
