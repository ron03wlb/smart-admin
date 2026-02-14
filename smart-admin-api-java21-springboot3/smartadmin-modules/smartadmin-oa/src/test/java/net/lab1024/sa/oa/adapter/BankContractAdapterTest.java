package net.lab1024.sa.oa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.oa.dto.BankDTO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.bank.domain.BankVO;
import net.lab1024.sa.oa.bank.service.BankService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BankContractAdapter 單元測試
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
class BankContractAdapterTest {

  @Mock private BankService bankService;

  @InjectMocks private BankContractAdapter adapter;

  // ==================== queryByEnterpriseId 測試 ====================

  @Test
  void testQueryByEnterpriseId_Success() {
    // Given
    Long enterpriseId = 1L;
    BankVO vo1 = new BankVO();
    vo1.setBankId(1L);
    vo1.setBankName("中國銀行");
    vo1.setEnterpriseId(enterpriseId);

    BankVO vo2 = new BankVO();
    vo2.setBankId(2L);
    vo2.setBankName("工商銀行");
    vo2.setEnterpriseId(enterpriseId);

    ResponseDTO<List<BankVO>> response = ResponseDTO.ok(Arrays.asList(vo1, vo2));
    when(bankService.queryList(enterpriseId)).thenReturn(response);

    // When
    List<BankDTO> result = adapter.queryByEnterpriseId(enterpriseId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBankName()).isEqualTo("中國銀行");
    assertThat(result.get(1).getBankName()).isEqualTo("工商銀行");
    verify(bankService).queryList(enterpriseId);
  }

  @Test
  void testQueryByEnterpriseId_EmptyResult() {
    // Given
    Long enterpriseId = 999L;
    ResponseDTO<List<BankVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(bankService.queryList(enterpriseId)).thenReturn(response);

    // When
    List<BankDTO> result = adapter.queryByEnterpriseId(enterpriseId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(bankService).queryList(enterpriseId);
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
    Long bankId = 1L;
    BankVO vo = new BankVO();
    vo.setBankId(bankId);
    vo.setBankName("中國銀行");
    vo.setAccountName("企業賬戶");
    vo.setAccountNumber("1234567890");

    ResponseDTO<BankVO> response = ResponseDTO.ok(vo);
    when(bankService.getDetail(bankId)).thenReturn(response);

    // When
    Option<BankDTO> result = adapter.getById(bankId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getBankId()).isEqualTo(bankId);
    assertThat(result.get().getBankName()).isEqualTo("中國銀行");
    assertThat(result.get().getAccountName()).isEqualTo("企業賬戶");
    verify(bankService).getDetail(bankId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long bankId = 999L;
    ResponseDTO<BankVO> response = ResponseDTO.ok(null);
    when(bankService.getDetail(bankId)).thenReturn(response);

    // When
    Option<BankDTO> result = adapter.getById(bankId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(bankService).getDetail(bankId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bankId cannot be null");
  }
}
