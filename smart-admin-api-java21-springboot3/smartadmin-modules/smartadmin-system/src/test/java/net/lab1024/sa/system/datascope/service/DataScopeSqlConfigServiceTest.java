package net.lab1024.sa.system.datascope.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.system.datascope.constant.DataScopeTypeEnum;
import net.lab1024.sa.system.datascope.constant.DataScopeViewTypeEnum;
import net.lab1024.sa.system.datascope.constant.DataScopeWhereInTypeEnum;
import net.lab1024.sa.system.datascope.domain.DataScopeSqlConfig;
import net.lab1024.sa.system.datascope.manager.DataScopeViewManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

/**
 * DataScopeSqlConfigService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>獲取SQL配置
 *   <li>組裝SQL（本人可見、員工範圍、部門範圍）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataScopeSqlConfigService 單元測試")
class DataScopeSqlConfigServiceTest {

  @Mock private DataScopeViewManager dataScopeViewManager;

  @Mock private ApplicationContext applicationContext;

  @InjectMocks private DataScopeSqlConfigService dataScopeSqlConfigService;

  // ==================== getSqlConfig 測試 ====================

  @Nested
  @DisplayName("getSqlConfig 獲取SQL配置測試")
  class GetSqlConfigTest {

    @Test
    @DisplayName("邊界情況：方法不存在時應返回null")
    void shouldReturnNullWhenMethodNotFound() {
      // When
      DataScopeSqlConfig result = dataScopeSqlConfigService.getSqlConfig("NonExistentClass.method");

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== getJoinSql 測試 ====================

  @Nested
  @DisplayName("getJoinSql 組裝SQL測試")
  class GetJoinSqlTest {

    @Test
    @DisplayName("邊界情況：未登錄時應返回空字符串")
    void shouldReturnEmptyWhenNotLoggedIn() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.EMPLOYEE);

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(null);

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEmpty();
      }
    }

    @Test
    @DisplayName("正常情況：本人可見時應返回create_user_id條件")
    void shouldReturnCreateUserIdWhenViewTypeMe() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.EMPLOYEE);
      Long employeeId = 1L;

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(employeeId);
        when(dataScopeViewManager.getEmployeeDataScopeViewType(any(), eq(employeeId)))
            .thenReturn(DataScopeViewTypeEnum.ME);

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEqualTo("create_user_id = 1");
      }
    }

    @Test
    @DisplayName("正常情況：員工範圍應替換員工ID佔位符")
    void shouldReplaceEmployeeIdsPlaceholder() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.EMPLOYEE);
      config.setJoinSql("employee_id IN (#employeeIds)");
      Long employeeId = 1L;

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(employeeId);
        when(dataScopeViewManager.getEmployeeDataScopeViewType(any(), eq(employeeId)))
            .thenReturn(DataScopeViewTypeEnum.DEPARTMENT);
        when(dataScopeViewManager.getCanViewEmployeeId(any(), eq(employeeId)))
            .thenReturn(List.of(1L, 2L, 3L));

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEqualTo("employee_id IN (1,2,3)");
      }
    }

    @Test
    @DisplayName("邊界情況：員工範圍無可見員工時返回空")
    void shouldReturnEmptyWhenNoViewableEmployees() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.EMPLOYEE);
      Long employeeId = 1L;

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(employeeId);
        when(dataScopeViewManager.getEmployeeDataScopeViewType(any(), eq(employeeId)))
            .thenReturn(DataScopeViewTypeEnum.DEPARTMENT);
        when(dataScopeViewManager.getCanViewEmployeeId(any(), eq(employeeId)))
            .thenReturn(Collections.emptyList());

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEmpty();
      }
    }

    @Test
    @DisplayName("正常情況：部門範圍應替換部門ID佔位符")
    void shouldReplaceDepartmentIdsPlaceholder() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.DEPARTMENT);
      config.setJoinSql("department_id IN (#departmentIds)");
      Long employeeId = 1L;

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(employeeId);
        when(dataScopeViewManager.getEmployeeDataScopeViewType(any(), eq(employeeId)))
            .thenReturn(DataScopeViewTypeEnum.DEPARTMENT);
        when(dataScopeViewManager.getCanViewDepartmentId(any(), eq(employeeId)))
            .thenReturn(List.of(10L, 20L));

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEqualTo("department_id IN (10,20)");
      }
    }

    @Test
    @DisplayName("邊界情況：部門範圍無可見部門時返回空")
    void shouldReturnEmptyWhenNoViewableDepartments() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.DEPARTMENT);
      Long employeeId = 1L;

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(employeeId);
        when(dataScopeViewManager.getEmployeeDataScopeViewType(any(), eq(employeeId)))
            .thenReturn(DataScopeViewTypeEnum.DEPARTMENT);
        when(dataScopeViewManager.getCanViewDepartmentId(any(), eq(employeeId)))
            .thenReturn(Collections.emptyList());

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEmpty();
      }
    }

    @Test
    @DisplayName("邊界情況：自定義策略類為null時應返回空")
    void shouldReturnEmptyWhenCustomStrategyClassNull() {
      // Given
      DataScopeSqlConfig config = createTestConfig(DataScopeWhereInTypeEnum.CUSTOM_STRATEGY);
      config.setJoinSqlImplClazz(null);
      Long employeeId = 1L;

      try (MockedStatic<SmartRequestUtil> mockedStatic = mockStatic(SmartRequestUtil.class)) {
        mockedStatic.when(SmartRequestUtil::getRequestUserId).thenReturn(employeeId);
        when(dataScopeViewManager.getEmployeeDataScopeViewType(any(), eq(employeeId)))
            .thenReturn(DataScopeViewTypeEnum.DEPARTMENT);

        // When
        String result = dataScopeSqlConfigService.getJoinSql(new HashMap<>(), config);

        // Then
        assertThat(result).isEmpty();
      }
    }
  }

  // ==================== Helper Methods ====================

  private DataScopeSqlConfig createTestConfig(DataScopeWhereInTypeEnum whereInType) {
    DataScopeSqlConfig config = new DataScopeSqlConfig();
    config.setDataScopeType(DataScopeTypeEnum.NOTICE);
    config.setDataScopeWhereInType(whereInType);
    config.setJoinSql("");
    config.setWhereIndex(0);
    config.setParamName("");
    return config;
  }
}
