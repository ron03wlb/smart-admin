package net.lab1024.sa.support.securityprotect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.service.SecurityConfigProvider;
import net.lab1024.sa.support.securityprotect.dao.LoginFailDao;
import net.lab1024.sa.support.securityprotect.domain.entity.LoginFailEntity;
import net.lab1024.sa.support.securityprotect.domain.form.LoginFailQueryForm;
import net.lab1024.sa.support.securityprotect.domain.vo.LoginFailVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SecurityLoginService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>checkLogin - 檢查是否可以登錄
 *   <li>recordLoginFail - 記錄登錄失敗
 *   <li>removeLoginFail - 清除登錄失敗記錄
 *   <li>queryPage - 分頁查詢
 *   <li>batchDelete - 批量刪除
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityLoginService 單元測試")
class SecurityLoginServiceTest {

  @Mock private SecurityConfigProvider securityConfigProvider;

  @Mock private LoginFailDao loginFailDao;

  @InjectMocks private SecurityLoginService securityLoginService;

  @Captor private ArgumentCaptor<LoginFailEntity> loginFailEntityCaptor;

  // ==================== checkLogin 測試 ====================

  @Nested
  @DisplayName("checkLogin 登錄檢查測試")
  class CheckLoginTest {

    @Test
    @DisplayName("最大失敗次數小於 1 時：應該直接返回成功")
    void shouldReturnOkWhenMaxTimesLessThan1() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(0);

      // When
      ResponseDTO<LoginFailEntity> result = securityLoginService.checkLogin(userId, userType);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(loginFailDao, never()).selectByUserIdAndUserType(any(), any());
    }

    @Test
    @DisplayName("無登錄失敗記錄時：應該返回成功")
    void shouldReturnOkWhenNoFailRecord() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(5);
      when(loginFailDao.selectByUserIdAndUserType(userId, userType.getValue())).thenReturn(null);

      // When
      ResponseDTO<LoginFailEntity> result = securityLoginService.checkLogin(userId, userType);

      // Then
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("失敗次數未達上限時：應該返回成功")
    void shouldReturnOkWhenBelowMaxTimes() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      LoginFailEntity failEntity = createTestLoginFailEntity();
      failEntity.setLoginFailCount(3);

      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(5);
      when(loginFailDao.selectByUserIdAndUserType(userId, userType.getValue()))
          .thenReturn(failEntity);

      // When
      ResponseDTO<LoginFailEntity> result = securityLoginService.checkLogin(userId, userType);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("達到上限但鎖定已過期時：應該返回成功")
    void shouldReturnOkWhenLockExpired() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      LoginFailEntity failEntity = createTestLoginFailEntity();
      failEntity.setLoginFailCount(5);
      failEntity.setLoginLockBeginTime(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(5);
      when(securityConfigProvider.getLoginFailLockSeconds()).thenReturn(1800); // 30 分鐘
      when(loginFailDao.selectByUserIdAndUserType(userId, userType.getValue()))
          .thenReturn(failEntity);

      // When
      ResponseDTO<LoginFailEntity> result = securityLoginService.checkLogin(userId, userType);

      // Then
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("達到上限且仍在鎖定期間：應該返回錯誤")
    void shouldReturnErrorWhenStillLocked() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      LoginFailEntity failEntity = createTestLoginFailEntity();
      failEntity.setLoginFailCount(5);
      failEntity.setLoginLockBeginTime(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));

      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(5);
      when(securityConfigProvider.getLoginFailLockSeconds()).thenReturn(1800); // 30 分鐘
      when(loginFailDao.selectByUserIdAndUserType(userId, userType.getValue()))
          .thenReturn(failEntity);

      // When
      ResponseDTO<LoginFailEntity> result = securityLoginService.checkLogin(userId, userType);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== removeLoginFail 測試 ====================

  @Nested
  @DisplayName("removeLoginFail 清除記錄測試")
  class RemoveLoginFailTest {

    @Test
    @DisplayName("最大失敗次數小於 1 時：不執行刪除")
    void shouldNotDeleteWhenMaxTimesLessThan1() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(0);

      // When
      securityLoginService.removeLoginFail(userId, userType);

      // Then
      verify(loginFailDao, never()).deleteByUserIdAndUserType(any(), any());
    }

    @Test
    @DisplayName("正常情況：應該刪除記錄")
    void shouldDeleteRecord() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      when(securityConfigProvider.getLoginFailMaxTimes()).thenReturn(5);

      // When
      securityLoginService.removeLoginFail(userId, userType);

      // Then
      verify(loginFailDao).deleteByUserIdAndUserType(userId, userType.getValue());
    }
  }

  // ==================== queryPage 測試 ====================

  @Nested
  @DisplayName("queryPage 分頁查詢測試")
  class QueryPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      LoginFailQueryForm form = new LoginFailQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      LoginFailVO vo = new LoginFailVO();
      when(loginFailDao.queryPage(any(Page.class), any(LoginFailQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      PageResult<LoginFailVO> result = securityLoginService.queryPage(form);

      // Then
      assertThat(result).isNotNull();
    }
  }

  // ==================== batchDelete 測試 ====================

  @Nested
  @DisplayName("batchDelete 批量刪除測試")
  class BatchDeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除")
    @SuppressWarnings("deprecation")
    void shouldDeleteSuccess() {
      // Given
      List<Long> idList = List.of(1L, 2L, 3L);

      // When
      ResponseDTO<String> result = securityLoginService.batchDelete(idList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(loginFailDao).deleteBatchIds(idList);
    }

    @Test
    @DisplayName("列表為空時：應該直接返回成功")
    void shouldReturnOkWhenListEmpty() {
      // When
      ResponseDTO<String> result = securityLoginService.batchDelete(List.of());

      // Then
      assertThat(result.getOk()).isTrue();
      verify(loginFailDao, never()).deleteBatchIds(any());
    }
  }

  // ==================== Helper Methods ====================

  private LoginFailEntity createTestLoginFailEntity() {
    return LoginFailEntity.builder()
        .loginFailId(1L)
        .userId(1L)
        .userType(UserTypeEnum.ADMIN_EMPLOYEE.getValue())
        .loginName("admin")
        .loginFailCount(1)
        .lockFlag(false)
        .build();
  }
}
