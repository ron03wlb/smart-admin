package net.lab1024.sa.oa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.oa.dto.EnterpriseDTO;
import net.lab1024.sa.api.oa.dto.EnterpriseEmployeeDTO;
import net.lab1024.sa.api.oa.dto.EnterpriseSimpleDTO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseEmployeeVO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseListVO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.oa.enterprise.service.EnterpriseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EnterpriseContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到、空列表）
 *   <li>Vavr Option 正確使用
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseContractAdapterTest {

  @Mock private EnterpriseService enterpriseService;

  @InjectMocks private EnterpriseContractAdapter adapter;

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long enterpriseId = 1L;
    EnterpriseVO vo = new EnterpriseVO();
    vo.setEnterpriseId(enterpriseId);
    vo.setEnterpriseName("智慧科技有限公司");
    vo.setType(1);
    vo.setUnifiedSocialCreditCode("91310000MA1K3EE00A");

    when(enterpriseService.getDetail(enterpriseId)).thenReturn(vo);

    // When
    Option<EnterpriseDTO> result = adapter.getById(enterpriseId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getEnterpriseId()).isEqualTo(enterpriseId);
    assertThat(result.get().getEnterpriseName()).isEqualTo("智慧科技有限公司");
    assertThat(result.get().getUnifiedSocialCreditCode()).isEqualTo("91310000MA1K3EE00A");
    verify(enterpriseService).getDetail(enterpriseId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long enterpriseId = 999L;
    when(enterpriseService.getDetail(enterpriseId)).thenReturn(null);

    // When
    Option<EnterpriseDTO> result = adapter.getById(enterpriseId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(enterpriseService).getDetail(enterpriseId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("enterpriseId cannot be null");
  }

  // ==================== queryByType 測試 ====================

  @Test
  void testQueryByType_Success() {
    // Given
    Integer type = 1;
    EnterpriseListVO vo1 = new EnterpriseListVO();
    vo1.setEnterpriseId(1L);
    vo1.setEnterpriseName("企業 A");

    EnterpriseListVO vo2 = new EnterpriseListVO();
    vo2.setEnterpriseId(2L);
    vo2.setEnterpriseName("企業 B");

    ResponseDTO<List<EnterpriseListVO>> response = ResponseDTO.ok(Arrays.asList(vo1, vo2));
    when(enterpriseService.queryList(type)).thenReturn(response);

    // When
    List<EnterpriseSimpleDTO> result = adapter.queryByType(type);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEnterpriseName()).isEqualTo("企業 A");
    assertThat(result.get(1).getEnterpriseName()).isEqualTo("企業 B");
    verify(enterpriseService).queryList(type);
  }

  @Test
  void testQueryByType_EmptyResult() {
    // Given
    Integer type = 999;
    ResponseDTO<List<EnterpriseListVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(enterpriseService.queryList(type)).thenReturn(response);

    // When
    List<EnterpriseSimpleDTO> result = adapter.queryByType(type);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(enterpriseService).queryList(type);
  }

  @Test
  void testQueryByType_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByType(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("type cannot be null");
  }

  // ==================== queryEmployeesByEnterpriseIds 測試 ====================

  @Test
  void testQueryEmployeesByEnterpriseIds_Success() {
    // Given
    List<Long> enterpriseIds = Arrays.asList(1L, 2L);

    EnterpriseEmployeeVO vo1 = new EnterpriseEmployeeVO();
    vo1.setEnterpriseId(1L);
    vo1.setEmployeeId(101L);
    vo1.setActualName("張三");
    vo1.setDepartmentName("技術部");

    EnterpriseEmployeeVO vo2 = new EnterpriseEmployeeVO();
    vo2.setEnterpriseId(2L);
    vo2.setEmployeeId(102L);
    vo2.setActualName("李四");
    vo2.setDepartmentName("市場部");

    List<EnterpriseEmployeeVO> voList = Arrays.asList(vo1, vo2);
    when(enterpriseService.employeeList(enterpriseIds)).thenReturn(voList);

    // When
    List<EnterpriseEmployeeDTO> result = adapter.queryEmployeesByEnterpriseIds(enterpriseIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEmployeeName()).isEqualTo("張三");
    assertThat(result.get(1).getEmployeeName()).isEqualTo("李四");
    verify(enterpriseService).employeeList(enterpriseIds);
  }

  @Test
  void testQueryEmployeesByEnterpriseIds_EmptyList() {
    // Given
    List<Long> enterpriseIds = Collections.emptyList();

    // When
    List<EnterpriseEmployeeDTO> result = adapter.queryEmployeesByEnterpriseIds(enterpriseIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testQueryEmployeesByEnterpriseIds_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryEmployeesByEnterpriseIds(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("enterpriseIds cannot be null");
  }

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    EnterpriseListVO vo1 = new EnterpriseListVO();
    vo1.setEnterpriseId(1L);
    vo1.setEnterpriseName("企業 A");

    EnterpriseListVO vo2 = new EnterpriseListVO();
    vo2.setEnterpriseId(2L);
    vo2.setEnterpriseName("企業 B");

    ResponseDTO<List<EnterpriseListVO>> response = ResponseDTO.ok(Arrays.asList(vo1, vo2));
    when(enterpriseService.queryList(null)).thenReturn(response);

    // When
    List<EnterpriseSimpleDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEnterpriseName()).isEqualTo("企業 A");
    assertThat(result.get(1).getEnterpriseName()).isEqualTo("企業 B");
    verify(enterpriseService).queryList(null);
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    ResponseDTO<List<EnterpriseListVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(enterpriseService.queryList(null)).thenReturn(response);

    // When
    List<EnterpriseSimpleDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(enterpriseService).queryList(null);
  }
}
