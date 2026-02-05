package net.lab1024.sa.oa.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.oa.contract.BankContract;
import net.lab1024.sa.api.oa.dto.BankDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.bank.service.BankService;
import org.springframework.stereotype.Component;

/**
 * 銀行契約適配器
 *
 * <p>Adapter Pattern: 將 BankService 適配到 BankContract API 契約。
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
public class BankContractAdapter implements BankContract {

  private final BankService bankService;

  /**
   * 根據企業 ID 查詢銀行列表
   *
   * @param enterpriseId 企業 ID
   * @return 銀行列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 enterpriseId 為 null
   */
  @Override
  public List<BankDTO> queryByEnterpriseId(Long enterpriseId) {
    if (enterpriseId == null) {
      throw new IllegalArgumentException("enterpriseId cannot be null");
    }
    return bankService.queryList(enterpriseId).getData().stream()
        .map(vo -> SmartBeanUtil.copy(vo, BankDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 根據 ID 查詢銀行詳情
   *
   * @param bankId 銀行 ID
   * @return Option 包裝的銀行對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 bankId 為 null
   */
  @Override
  public Option<BankDTO> getById(Long bankId) {
    if (bankId == null) {
      throw new IllegalArgumentException("bankId cannot be null");
    }
    return Option.of(bankService.getDetail(bankId).getData())
        .map(vo -> SmartBeanUtil.copy(vo, BankDTO.class));
  }
}
