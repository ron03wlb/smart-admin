package net.lab1024.sa.oa.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.oa.contract.EnterpriseContract;
import net.lab1024.sa.api.oa.dto.EnterpriseDTO;
import net.lab1024.sa.api.oa.dto.EnterpriseEmployeeDTO;
import net.lab1024.sa.api.oa.dto.EnterpriseSimpleDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.enterprise.service.EnterpriseService;
import org.springframework.stereotype.Component;

/**
 * 企業契約適配器
 *
 * <p>Adapter Pattern: 將 EnterpriseService 適配到 EnterpriseContract API 契約。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 替代 null 返回
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
public class EnterpriseContractAdapter implements EnterpriseContract {

  private final EnterpriseService enterpriseService;

  /**
   * 根據 ID 查詢企業詳情
   *
   * @param enterpriseId 企業 ID
   * @return Option 包裝的企業對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 enterpriseId 為 null
   */
  @Override
  public Option<EnterpriseDTO> getById(Long enterpriseId) {
    if (enterpriseId == null) {
      throw new IllegalArgumentException("enterpriseId cannot be null");
    }
    return Option.of(enterpriseService.getDetail(enterpriseId))
        .map(vo -> SmartBeanUtil.copy(vo, EnterpriseDTO.class));
  }

  /**
   * 根據類型查詢企業列表
   *
   * @param type 企業類型
   * @return 企業列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 type 為 null
   */
  @Override
  public List<EnterpriseSimpleDTO> queryByType(Integer type) {
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    return enterpriseService.queryList(type).getData().stream()
        .map(vo -> SmartBeanUtil.copy(vo, EnterpriseSimpleDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 批量查詢企業員工關聯
   *
   * @param enterpriseIds 企業 ID 列表
   * @return 企業員工關聯列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 enterpriseIds 為 null
   */
  @Override
  public List<EnterpriseEmployeeDTO> queryEmployeesByEnterpriseIds(List<Long> enterpriseIds) {
    if (enterpriseIds == null) {
      throw new IllegalArgumentException("enterpriseIds cannot be null");
    }
    if (enterpriseIds.isEmpty()) {
      return List.of();
    }
    return enterpriseService.employeeList(enterpriseIds).stream()
        .map(
            vo -> {
              EnterpriseEmployeeDTO dto = SmartBeanUtil.copy(vo, EnterpriseEmployeeDTO.class);
              // Manual mapping: actualName → employeeName
              dto.setEmployeeName(vo.getActualName());
              return dto;
            })
        .collect(Collectors.toList());
  }

  /**
   * 查詢所有企業（非刪除）
   *
   * @return 企業列表（非空，可能為空列表）
   */
  @Override
  public List<EnterpriseSimpleDTO> listAll() {
    return enterpriseService.queryList(null).getData().stream()
        .map(vo -> SmartBeanUtil.copy(vo, EnterpriseSimpleDTO.class))
        .collect(Collectors.toList());
  }
}
