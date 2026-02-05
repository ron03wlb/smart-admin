package net.lab1024.sa.system.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.system.dto.EmployeeDTO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.system.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EmployeeContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到）
 *   <li>Vavr Option 正確使用
 *   <li>SmartBeanUtil 轉換驗證
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class EmployeeContractAdapterTest {

  @Mock private EmployeeService employeeService;

  @Mock private EmployeeDao employeeDao;

  @InjectMocks private EmployeeContractAdapter adapter;

  // ==================== queryByDepartmentId 測試 ====================

  @Test
  void testQueryByDepartmentId_Success() {
    // Given
    Long departmentId = 10L;
    EmployeeVO vo1 = new EmployeeVO();
    vo1.setEmployeeId(1L);
    vo1.setActualName("張三");
    vo1.setDepartmentId(departmentId);

    EmployeeVO vo2 = new EmployeeVO();
    vo2.setEmployeeId(2L);
    vo2.setActualName("李四");
    vo2.setDepartmentId(departmentId);

    List<EmployeeVO> voList = Arrays.asList(vo1, vo2);
    ResponseDTO<List<EmployeeVO>> response = ResponseDTO.ok(voList);
    when(employeeService.getAllEmployeeByDepartmentId(departmentId)).thenReturn(response);

    // When
    List<EmployeeDTO> result = adapter.queryByDepartmentId(departmentId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEmployeeId()).isEqualTo(1L);
    assertThat(result.get(0).getActualName()).isEqualTo("張三");
    assertThat(result.get(1).getEmployeeId()).isEqualTo(2L);
    verify(employeeService).getAllEmployeeByDepartmentId(departmentId);
  }

  @Test
  void testQueryByDepartmentId_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByDepartmentId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("departmentId cannot be null");
  }

  @Test
  void testQueryByDepartmentId_EmptyResult() {
    // Given
    Long departmentId = 999L;
    ResponseDTO<List<EmployeeVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(employeeService.getAllEmployeeByDepartmentId(departmentId)).thenReturn(response);

    // When
    List<EmployeeDTO> result = adapter.queryByDepartmentId(departmentId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  // ==================== queryAll 測試 ====================

  @Test
  void testQueryAll_WithDisabledFlag() {
    // Given
    Boolean disabledFlag = false;
    EmployeeVO vo = new EmployeeVO();
    vo.setEmployeeId(1L);
    vo.setActualName("王五");
    vo.setDisabledFlag(false);

    ResponseDTO<List<EmployeeVO>> response = ResponseDTO.ok(Collections.singletonList(vo));
    when(employeeService.queryAllEmployee(disabledFlag)).thenReturn(response);

    // When
    List<EmployeeDTO> result = adapter.queryAll(disabledFlag);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmployeeId()).isEqualTo(1L);
    assertThat(result.get(0).getDisabledFlag()).isFalse();
    verify(employeeService).queryAllEmployee(disabledFlag);
  }

  @Test
  void testQueryAll_WithNullDisabledFlag() {
    // Given
    ResponseDTO<List<EmployeeVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(employeeService.queryAllEmployee(null)).thenReturn(response);

    // When
    List<EmployeeDTO> result = adapter.queryAll(null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testQueryAll_EmptyResult() {
    // Given
    Boolean disabledFlag = true;
    ResponseDTO<List<EmployeeVO>> response = ResponseDTO.ok(Collections.emptyList());
    when(employeeService.queryAllEmployee(disabledFlag)).thenReturn(response);

    // When
    List<EmployeeDTO> result = adapter.queryAll(disabledFlag);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  // ==================== getByLoginName 測試 ====================

  @Test
  void testGetByLoginName_Found() {
    // Given
    String loginName = "zhangsan";
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(1L);
    entity.setLoginName(loginName);
    entity.setActualName("張三");

    when(employeeService.getByLoginName(loginName)).thenReturn(entity);

    // When
    Option<EmployeeDTO> result = adapter.getByLoginName(loginName);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getEmployeeId()).isEqualTo(1L);
    assertThat(result.get().getLoginName()).isEqualTo(loginName);
    assertThat(result.get().getActualName()).isEqualTo("張三");
    verify(employeeService).getByLoginName(loginName);
  }

  @Test
  void testGetByLoginName_NotFound() {
    // Given
    String loginName = "nonexistent";
    when(employeeService.getByLoginName(loginName)).thenReturn(null);

    // When
    Option<EmployeeDTO> result = adapter.getByLoginName(loginName);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetByLoginName_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getByLoginName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("loginName cannot be null or blank");
  }

  @Test
  void testGetByLoginName_BlankParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getByLoginName("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("loginName cannot be null or blank");
  }

  // ==================== queryByIds 測試 ====================

  @Test
  void testQueryByIds_Success() {
    // Given
    List<Long> employeeIds = Arrays.asList(1L, 2L, 3L);
    EmployeeEntity entity1 = new EmployeeEntity();
    entity1.setEmployeeId(1L);
    entity1.setActualName("張三");

    EmployeeEntity entity2 = new EmployeeEntity();
    entity2.setEmployeeId(2L);
    entity2.setActualName("李四");

    List<EmployeeEntity> entities = Arrays.asList(entity1, entity2);
    when(employeeDao.selectBatchIds(employeeIds)).thenReturn(entities);

    // When
    List<EmployeeDTO> result = adapter.queryByIds(employeeIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEmployeeId()).isEqualTo(1L);
    assertThat(result.get(1).getEmployeeId()).isEqualTo(2L);
    verify(employeeDao).selectBatchIds(employeeIds);
  }

  @Test
  void testQueryByIds_EmptyList() {
    // Given
    List<Long> employeeIds = Collections.emptyList();

    // When
    List<EmployeeDTO> result = adapter.queryByIds(employeeIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testQueryByIds_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByIds(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("employeeIds cannot be null");
  }

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long employeeId = 1L;
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(employeeId);
    entity.setActualName("張三");
    entity.setLoginName("zhangsan");

    when(employeeService.getById(employeeId)).thenReturn(entity);

    // When
    Option<EmployeeDTO> result = adapter.getById(employeeId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getEmployeeId()).isEqualTo(employeeId);
    assertThat(result.get().getActualName()).isEqualTo("張三");
    assertThat(result.get().getLoginName()).isEqualTo("zhangsan");
    verify(employeeService).getById(employeeId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long employeeId = 999L;
    when(employeeService.getById(employeeId)).thenReturn(null);

    // When
    Option<EmployeeDTO> result = adapter.getById(employeeId);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("employeeId cannot be null");
  }
}
