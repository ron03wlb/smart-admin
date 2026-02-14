package net.lab1024.sa.oa.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.oa.contract.InvoiceContract;
import net.lab1024.sa.api.oa.dto.InvoiceDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.invoice.service.InvoiceService;
import org.springframework.stereotype.Component;

/**
 * 發票契約適配器
 *
 * <p>Adapter Pattern: 將 InvoiceService 適配到 InvoiceContract API 契約。
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
public class InvoiceContractAdapter implements InvoiceContract {

  private final InvoiceService invoiceService;

  /**
   * 根據企業 ID 查詢發票列表
   *
   * @param enterpriseId 企業 ID
   * @return 發票列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 enterpriseId 為 null
   */
  @Override
  public List<InvoiceDTO> queryByEnterpriseId(Long enterpriseId) {
    if (enterpriseId == null) {
      throw new IllegalArgumentException("enterpriseId cannot be null");
    }
    return invoiceService.queryList(enterpriseId).getData().stream()
        .map(vo -> SmartBeanUtil.copy(vo, InvoiceDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 根據 ID 查詢發票詳情
   *
   * @param invoiceId 發票 ID
   * @return Option 包裝的發票對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 invoiceId 為 null
   */
  @Override
  public Option<InvoiceDTO> getById(Long invoiceId) {
    if (invoiceId == null) {
      throw new IllegalArgumentException("invoiceId cannot be null");
    }
    return Option.of(invoiceService.getDetail(invoiceId).getData())
        .map(vo -> SmartBeanUtil.copy(vo, InvoiceDTO.class));
  }
}
