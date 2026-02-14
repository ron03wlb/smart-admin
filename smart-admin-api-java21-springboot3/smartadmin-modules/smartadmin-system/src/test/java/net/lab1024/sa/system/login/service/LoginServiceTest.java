package net.lab1024.sa.system.login.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpUtil;
import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.common.apiencrypt.service.ApiEncryptService;
import net.lab1024.sa.common.cache.CacheService;
import net.lab1024.sa.common.captcha.CaptchaService;
import net.lab1024.sa.common.captcha.CaptchaVO;
import net.lab1024.sa.common.core.domain.UserPermission;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.config.ConfigService;
import net.lab1024.sa.support.loginlog.LoginLogService;
import net.lab1024.sa.support.mail.service.MailService;
import net.lab1024.sa.support.securityprotect.service.Level3ProtectConfigService;
import net.lab1024.sa.support.securityprotect.service.SecurityLoginService;
import net.lab1024.sa.support.securityprotect.service.SecurityPasswordService;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.login.domain.LoginForm;
import net.lab1024.sa.system.login.domain.RequestEmployee;
import net.lab1024.sa.system.login.manager.LoginManager;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * LoginService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>驗證碼獲取
 *   <li>員工登錄（密碼驗證、狀態驗證）
 *   <li>員工登出
 *   <li>權限列表獲取
 *   <li>郵箱驗證碼發送
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoginService 單元測試")
class LoginServiceTest {

  @Mock private EmployeeDao employeeDao;

  @Mock private CaptchaService captchaService;

  @Mock private ConfigService configService;

  @Mock private LoginLogService loginLogService;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleMenuDao roleMenuDao;

  @Mock private SecurityLoginService securityLoginService;

  @Mock private SecurityPasswordService protectPasswordService;

  @Mock private ApiEncryptService apiEncryptService;

  @Mock private Level3ProtectConfigService level3ProtectConfigService;

  @Mock private MailService mailService;

  @Mock private CacheService cacheService;

  @Mock private LoginManager loginManager;

  @Spy @InjectMocks private LoginService loginService;

  // ==================== getCaptcha 測試 ====================

  @Nested
  @DisplayName("getCaptcha 獲取驗證碼測試")
  class GetCaptchaTest {

    @Test
    @DisplayName("正常情況：應該返回驗證碼")
    void shouldReturnCaptcha() {
      // Given
      CaptchaVO captchaVO = new CaptchaVO();
      captchaVO.setCaptchaUuid("uuid-123");
      captchaVO.setCaptchaBase64Image("base64image");
      when(captchaService.generateCaptcha()).thenReturn(captchaVO);

      // When
      ResponseDTO<CaptchaVO> result = loginService.getCaptcha();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getCaptchaUuid()).isEqualTo("uuid-123");
    }
  }

  // ==================== login 測試 ====================

  @Nested
  @DisplayName("login 員工登錄測試")
  class LoginTest {

    @Test
    @DisplayName("異常情況：登錄設備不支持時應返回錯誤")
    void shouldReturnErrorWhenDeviceNotSupported() {
      // Given
      LoginForm loginForm = createTestLoginForm("admin", "password");
      loginForm.setLoginDevice(999); // 不支持的設備

      // When
      ResponseDTO<?> result = loginService.login(loginForm, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("登录设备暂不支持");
    }

    @Test
    @DisplayName("異常情況：登錄名不存在時應返回錯誤")
    void shouldReturnErrorWhenLoginNameNotFound() {
      // Given
      LoginForm loginForm = createTestLoginForm("nonexistent", "password");
      loginForm.setLoginDevice(1);

      when(employeeDao.getByLoginName("nonexistent", false)).thenReturn(null);

      // When
      ResponseDTO<?> result = loginService.login(loginForm, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("登录名或密码错误");
    }

    @Test
    @DisplayName("異常情況：賬號已刪除時應返回錯誤")
    void shouldReturnErrorWhenAccountDeleted() {
      // Given
      LoginForm loginForm = createTestLoginForm("admin", "password");
      loginForm.setLoginDevice(1);

      EmployeeEntity deletedEmployee = createTestEmployee(1L, "admin", true, false);

      when(employeeDao.getByLoginName("admin", false)).thenReturn(deletedEmployee);

      // When
      ResponseDTO<?> result = loginService.login(loginForm, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("账号已被删除");
    }

    @Test
    @DisplayName("異常情況：賬號已禁用時應返回錯誤")
    void shouldReturnErrorWhenAccountDisabled() {
      // Given
      LoginForm loginForm = createTestLoginForm("admin", "password");
      loginForm.setLoginDevice(1);

      EmployeeEntity disabledEmployee = createTestEmployee(1L, "admin", false, true);

      when(employeeDao.getByLoginName("admin", false)).thenReturn(disabledEmployee);

      // When
      ResponseDTO<?> result = loginService.login(loginForm, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("账号已被禁用");
    }

    @Test
    @DisplayName("異常情況：密碼解密失敗時應返回錯誤")
    void shouldReturnErrorWhenDecryptFailed() {
      // Given
      LoginForm loginForm = createTestLoginForm("admin", "encrypted");
      loginForm.setLoginDevice(1);

      EmployeeEntity employee = createTestEmployee(1L, "admin", false, false);

      when(employeeDao.getByLoginName("admin", false)).thenReturn(employee);
      when(apiEncryptService.decrypt("encrypted")).thenReturn(null);

      // When
      ResponseDTO<?> result = loginService.login(loginForm, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("密码解密失败");
    }
  }

  // ==================== logout 測試 ====================

  @Nested
  @DisplayName("logout 員工登出測試")
  class LogoutTest {

    @Test
    @DisplayName("正常情況：應該成功登出並清除緩存")
    void shouldLogoutAndClearCache() {
      // Given
      RequestUser requestUser = createTestRequestUser(1L, "admin");

      try (MockedStatic<StpUtil> mockedStpUtil = mockStatic(StpUtil.class)) {
        // When
        ResponseDTO<String> result = loginService.logout(requestUser);

        // Then
        assertThat(result.getOk()).isTrue();
        mockedStpUtil.verify(StpUtil::logout);
        verify(loginManager).clearUserPermission(1L);
        verify(loginManager).clearUserLoginInfo(1L);
      }
    }
  }

  // ==================== getEmployeeIdByLoginId 測試 ====================

  @Nested
  @DisplayName("getEmployeeIdByLoginId 解析員工ID測試")
  class GetEmployeeIdByLoginIdTest {

    @Test
    @DisplayName("正常情況：普通登錄ID應該正確解析")
    void shouldParseNormalLoginId() {
      // Given
      String loginId = "1:12345";

      // When
      Long result = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertThat(result).isEqualTo(12345L);
    }

    @Test
    @DisplayName("正常情況：萬能密碼登錄ID應該正確解析")
    void shouldParseSuperPasswordLoginId() {
      // Given
      String loginId = "S:abc123:67890";

      // When
      Long result = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertThat(result).isEqualTo(67890L);
    }

    @Test
    @DisplayName("邊界情況：無效格式應返回null")
    void shouldReturnNullForInvalidFormat() {
      // Given
      String loginId = "invalid";

      // When
      Long result = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("邊界情況：空loginId應返回null")
    void shouldReturnNullForNullLoginId() {
      // When
      Long result = loginService.getEmployeeIdByLoginId(null);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("邊界情況：過短的loginId應返回null")
    void shouldReturnNullForShortLoginId() {
      // Given
      String loginId = "ab";

      // When
      Long result = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== getPermissionList 測試 ====================

  @Nested
  @DisplayName("getPermissionList 獲取權限列表測試")
  class GetPermissionListTest {

    @Test
    @DisplayName("正常情況：應該返回權限列表")
    void shouldReturnPermissionList() {
      // Given
      String loginId = "1:12345";
      UserPermission userPermission = new UserPermission();
      userPermission.setPermissionList(List.of("user:add", "user:edit"));

      doReturn(12345L).when(loginService).getEmployeeIdByLoginId(loginId);
      when(loginManager.getUserPermission(12345L)).thenReturn(userPermission);

      // When
      List<String> result = loginService.getPermissionList(loginId, "login");

      // Then
      assertThat(result).containsExactly("user:add", "user:edit");
    }

    @Test
    @DisplayName("邊界情況：員工ID為null時應返回空列表")
    void shouldReturnEmptyListWhenEmployeeIdNull() {
      // Given
      String loginId = "invalid";
      doReturn(null).when(loginService).getEmployeeIdByLoginId(loginId);

      // When
      List<String> result = loginService.getPermissionList(loginId, "login");

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== getRoleList 測試 ====================

  @Nested
  @DisplayName("getRoleList 獲取角色列表測試")
  class GetRoleListTest {

    @Test
    @DisplayName("正常情況：應該返回角色列表")
    void shouldReturnRoleList() {
      // Given
      String loginId = "1:12345";
      UserPermission userPermission = new UserPermission();
      userPermission.setRoleList(List.of("admin", "manager"));

      doReturn(12345L).when(loginService).getEmployeeIdByLoginId(loginId);
      when(loginManager.getUserPermission(12345L)).thenReturn(userPermission);

      // When
      List<String> result = loginService.getRoleList(loginId, "login");

      // Then
      assertThat(result).containsExactly("admin", "manager");
    }
  }

  // ==================== sendEmailCode 測試 ====================

  @Nested
  @DisplayName("sendEmailCode 發送郵箱驗證碼測試")
  class SendEmailCodeTest {

    @Test
    @DisplayName("異常情況：登錄名格式無效時應返回錯誤")
    void shouldReturnErrorWhenLoginNameInvalid() {
      // Given
      String invalidLoginName = "ab"; // 少於3個字符

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(invalidLoginName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("登錄名必須為 3-50 個字符");
    }

    @Test
    @DisplayName("異常情況：雙因素登錄未開啟時應返回錯誤")
    void shouldReturnErrorWhenTwoFactorDisabled() {
      // Given
      String loginName = "admin";
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(false);

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(loginName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("无需使用邮箱验证码");
    }

    @Test
    @DisplayName("正常情況：登錄名不存在時應返回成功（安全考慮）")
    void shouldReturnOkWhenLoginNameNotExists() {
      // Given
      String loginName = "nonexistent";
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName(loginName, false)).thenReturn(null);

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(loginName);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(mailService, never()).sendMail(any(), any(), anyList());
    }

    @Test
    @DisplayName("異常情況：賬號已刪除時應返回錯誤")
    void shouldReturnErrorWhenAccountDeletedForEmail() {
      // Given
      String loginName = "admin";
      EmployeeEntity deletedEmployee = createTestEmployee(1L, "admin", true, false);

      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName(loginName, false)).thenReturn(deletedEmployee);

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(loginName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("账号已被删除");
    }

    @Test
    @DisplayName("異常情況：未配置郵箱時應返回錯誤")
    void shouldReturnErrorWhenNoEmailConfigured() {
      // Given
      String loginName = "admin";
      EmployeeEntity employee = createTestEmployee(1L, "admin", false, false);
      employee.setEmail(null);

      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName(loginName, false)).thenReturn(employee);

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(loginName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("暂未配置邮箱地址");
    }

    @Test
    @DisplayName("異常情況：60秒內重複發送應返回錯誤")
    void shouldReturnErrorWhenResendWithin60Seconds() {
      // Given
      String loginName = "admin";
      EmployeeEntity employee = createTestEmployee(1L, "admin", false, false);
      employee.setEmail("admin@example.com");

      // 緩存中存在驗證碼，發送時間在60秒內
      String cachedCode = "1234_" + System.currentTimeMillis();

      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName(loginName, false)).thenReturn(employee);
      when(cacheService.get(any(), anyString(), eq(String.class)))
          .thenReturn(Option.of(cachedCode));

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(loginName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("一分钟内请勿重复发送");
    }

    @Test
    @DisplayName("正常情況：應該成功發送郵箱驗證碼")
    void shouldSendEmailCodeSuccess() {
      // Given
      String loginName = "admin";
      EmployeeEntity employee = createTestEmployee(1L, "admin", false, false);
      employee.setEmail("admin@example.com");

      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName(loginName, false)).thenReturn(employee);
      when(cacheService.get(any(), anyString(), eq(String.class))).thenReturn(Option.none());
      when(mailService.sendMail(any(), any(), anyList())).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = loginService.sendEmailCode(loginName);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(mailService).sendMail(any(), any(), anyList());
    }
  }

  // ==================== clearLoginEmployeeCache 測試 ====================

  @Nested
  @DisplayName("clearLoginEmployeeCache 清除緩存測試")
  class ClearLoginEmployeeCacheTest {

    @Test
    @DisplayName("正常情況：應該清除用戶權限和登錄信息緩存")
    void shouldClearAllCache() {
      // Given
      Long employeeId = 1L;

      // When
      loginService.clearLoginEmployeeCache(employeeId);

      // Then
      verify(loginManager).clearUserPermission(employeeId);
      verify(loginManager).clearUserLoginInfo(employeeId);
    }
  }

  // ==================== Helper Methods ====================

  private LoginForm createTestLoginForm(String loginName, String password) {
    LoginForm form = new LoginForm();
    form.setLoginName(loginName);
    form.setPassword(password);
    form.setCaptchaUuid("uuid");
    form.setCaptchaCode("1234");
    return form;
  }

  private EmployeeEntity createTestEmployee(
      Long id, String loginName, boolean deleted, boolean disabled) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(id);
    entity.setLoginName(loginName);
    entity.setActualName("員工" + id);
    entity.setEmployeeUid("UID" + id);
    entity.setDeletedFlag(deleted);
    entity.setDisabledFlag(disabled);
    entity.setLoginPwd("hashedPassword");
    return entity;
  }

  private RequestUser createTestRequestUser(Long employeeId, String actualName) {
    RequestEmployee requestEmployee = new RequestEmployee();
    requestEmployee.setEmployeeId(employeeId);
    requestEmployee.setActualName(actualName);
    requestEmployee.setUserType(UserTypeEnum.ADMIN_EMPLOYEE);
    requestEmployee.setIp("127.0.0.1");
    requestEmployee.setUserAgent("Mozilla");
    return requestEmployee;
  }
}
