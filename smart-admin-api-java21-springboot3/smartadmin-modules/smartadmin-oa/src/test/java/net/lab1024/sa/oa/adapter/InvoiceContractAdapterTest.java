package net.lab1024.sa.oa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.oa.dto.InvoiceDTO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.invoice.domain.InvoiceVO;
import net.lab1024.sa.oa.invoice.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * InvoiceContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到）
 *   <li>Vavr Option 正確使用
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class InvoiceContractAdapterTest {

  @Mock private InvoiceService invoiceService;

  @InjectMocks private InvoiceContractAdapter adapter;

  // ==================== queryByEnterpriseId 測試 ====================

  @Test
  void testQueryByEnterpriseId_Success() {
    // Given
    Long enterpriseId = 1L;
    InvoiceVO vo1 = new InvoiceVO();
    vo1.setInvoiceId(1L);
    vo1.setInvoiceHeads("企業發票 A");
    vo1.setEnterpriseId(enterpriseId);

    InvoiceVO vo2 = new InvoiceVO();
    vo2.setInvoiceId(2L);
    vo2.setInvoiceHeads("企業發票 B");
    vo2.setEnterpriseId(enterpriseId);

    ResponseDTO<List<InvoiceVO>> response = ResponseDTO.ok(Arrays.asList(vo1, vo2));
    when(invoiceService.queryList(enterpriseId)).thenReturn(response);

    // When
    List<InvoiceDTO> result = adapter.queryByEnterpriseId(enterpriseId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getInvoiceHeads()).isEqualTo("企業發票 A");
    assertThat(result.get(1).getInvoiceHeads()).isEqualTo("企業發票 B");
    verify(invoiceService).queryList(enterpriseId);
  }

  @Test
  void testQueryByEnterpriseId_EmptyResult() {
    // Given
    Long enterpriseId = 999L;
    ResponseDTO<List<InvoiceVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(invoiceService.queryList(enterpriseId)).thenReturn(response);

    // When
    List<InvoiceDTO> result = adapter.queryByEnterpriseId(enterpriseId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(invoiceService).queryList(enterpriseId);
  }

  @Test
  void testQueryByEnterpriseId_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByEnterpriseId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("enterpriseId cannot be null");
  }

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long invoiceId = 1L;
    InvoiceVO vo = new InvoiceVO();
    vo.setInvoiceId(invoiceId);
    vo.setInvoiceHeads("企業發票");
    vo.setTaxpayerIdentificationNumber("91310000MA1K3EE00A");
    vo.setBankName("中國銀行");

    ResponseDTO<InvoiceVO> response = ResponseDTO.ok(vo);
    when(invoiceService.getDetail(invoiceId)).thenReturn(response);

    // When
    Option<InvoiceDTO> result = adapter.getById(invoiceId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getInvoiceId()).isEqualTo(invoiceId);
    assertThat(result.get().getInvoiceHeads()).isEqualTo("企業發票");
    assertThat(result.get().getTaxpayerIdentificationNumber()).isEqualTo("91310000MA1K3EE00A");
    verify(invoiceService).getDetail(invoiceId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long invoiceId = 999L;
    ResponseDTO<InvoiceVO> response = ResponseDTO.ok(null);
    when(invoiceService.getDetail(invoiceId)).thenReturn(response);

    // When
    Option<InvoiceDTO> result = adapter.getById(invoiceId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(invoiceService).getDetail(invoiceId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invoiceId cannot be null");
  }
}
