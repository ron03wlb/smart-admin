package net.lab1024.sa.support.loginlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.loginlog.domain.LoginLogEntity;
import net.lab1024.sa.support.loginlog.domain.LoginLogQueryForm;
import net.lab1024.sa.support.loginlog.domain.LoginLogVO;
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
 * LoginLogService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryByPage - 分頁查詢登錄日誌
 *   <li>log - 記錄登錄日誌（含異常處理）
 *   <li>queryLastByUserId - 查詢用戶最後一次登錄記錄
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginLogService 單元測試")
class LoginLogServiceTest {

  @Mock private LoginLogDao loginLogDao;

  @InjectMocks private LoginLogService loginLogService;

  @Captor private ArgumentCaptor<LoginLogEntity> loginLogEntityCaptor;

  // ==================== queryByPage 測試 ====================

  @Nested
  @DisplayName("queryByPage 分頁查詢測試")
  class QueryByPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      LoginLogQueryForm form = new LoginLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      LoginLogVO vo = createTestLoginLogVO();
      when(loginLogDao.queryByPage(any(Page.class), any(LoginLogQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      ResponseDTO<PageResult<LoginLogVO>> result = loginLogService.queryByPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      LoginLogQueryForm form = new LoginLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(loginLogDao.queryByPage(any(Page.class), any(LoginLogQueryForm.class)))
          .thenReturn(List.of());

      // When
      ResponseDTO<PageResult<LoginLogVO>> result = loginLogService.queryByPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }
  }

  // ==================== log 測試 ====================

  @Nested
  @DisplayName("log 記錄登錄日誌測試")
  class LogTest {

    @Test
    @DisplayName("正常情況：應該成功插入日誌")
    void shouldInsertLogSuccessfully() {
      // Given
      LoginLogEntity entity = createTestLoginLogEntity();
      when(loginLogDao.insert(any(LoginLogEntity.class))).thenReturn(1);

      // When
      loginLogService.log(entity);

      // Then
      verify(loginLogDao).insert(loginLogEntityCaptor.capture());
      LoginLogEntity captured = loginLogEntityCaptor.getValue();
      assertThat(captured.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("異常情況：插入失敗時不應拋出異常（吞沒異常）")
    void shouldNotThrowExceptionWhenInsertFails() {
      // Given
      LoginLogEntity entity = createTestLoginLogEntity();
      when(loginLogDao.insert(any(LoginLogEntity.class)))
          .thenThrow(new RuntimeException("DB error"));

      // When & Then - 不應拋出異常
      loginLogService.log(entity);

      // 驗證確實嘗試了插入
      verify(loginLogDao).insert(any(LoginLogEntity.class));
    }
  }

  // ==================== queryLastByUserId 測試 ====================

  @Nested
  @DisplayName("queryLastByUserId 查詢最後登錄測試")
  class QueryLastByUserIdTest {

    @Test
    @DisplayName("正常情況：應該返回最後一次登錄記錄")
    void shouldReturnLastLoginRecord() {
      // Given
      Long userId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      LoginLogResultEnum result = LoginLogResultEnum.LOGIN_SUCCESS;

      LoginLogVO vo = createTestLoginLogVO();
      when(loginLogDao.queryLastByUserId(userId, userType.getValue(), result.getValue()))
          .thenReturn(vo);

      // When
      LoginLogVO loginLogVO = loginLogService.queryLastByUserId(userId, userType, result);

      // Then
      assertThat(loginLogVO).isNotNull();
      assertThat(loginLogVO.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("無記錄：應該返回 null")
    void shouldReturnNullWhenNoRecord() {
      // Given
      Long userId = 999L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      LoginLogResultEnum result = LoginLogResultEnum.LOGIN_SUCCESS;

      when(loginLogDao.queryLastByUserId(userId, userType.getValue(), result.getValue()))
          .thenReturn(null);

      // When
      LoginLogVO loginLogVO = loginLogService.queryLastByUserId(userId, userType, result);

      // Then
      assertThat(loginLogVO).isNull();
    }
  }

  // ==================== Helper Methods ====================

  private LoginLogEntity createTestLoginLogEntity() {
    return LoginLogEntity.builder()
        .loginLogId(1L)
        .userId(1L)
        .userType(UserTypeEnum.ADMIN_EMPLOYEE.getValue())
        .userName("admin")
        .loginIp("127.0.0.1")
        .loginIpRegion("本機")
        .loginDevice("Chrome")
        .loginResult(LoginLogResultEnum.LOGIN_SUCCESS.getValue())
        .createTime(LocalDateTime.now())
        .build();
  }

  private LoginLogVO createTestLoginLogVO() {
    LoginLogVO vo = new LoginLogVO();
    vo.setLoginLogId(1L);
    vo.setUserId(1L);
    vo.setUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
    vo.setUserName("admin");
    vo.setLoginIp("127.0.0.1");
    vo.setLoginResult(LoginLogResultEnum.LOGIN_SUCCESS.getValue());
    vo.setCreateTime(LocalDateTime.now());
    return vo;
  }
}
