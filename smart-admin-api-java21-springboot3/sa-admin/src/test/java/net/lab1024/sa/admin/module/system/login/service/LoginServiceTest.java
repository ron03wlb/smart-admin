package net.lab1024.sa.admin.module.system.login.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.fixtures.EmployeeTestFixture;
import net.lab1024.sa.admin.module.support.securityprotect.domain.entity.LoginFailEntity;
import net.lab1024.sa.admin.module.support.securityprotect.service.Level3ProtectConfigService;
import net.lab1024.sa.admin.module.support.securityprotect.service.SecurityLoginService;
import net.lab1024.sa.admin.module.support.securityprotect.service.SecurityPasswordService;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.login.domain.LoginForm;
import net.lab1024.sa.admin.module.system.login.domain.LoginResultVO;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.module.system.login.manager.LoginManager;
import net.lab1024.sa.admin.module.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;
import net.lab1024.sa.base.constant.LoginDeviceEnum;
import net.lab1024.sa.base.core.domain.UserPermission;
import net.lab1024.sa.base.module.support.config.ConfigKeyEnum;
import net.lab1024.sa.base.module.support.config.ConfigService;
import net.lab1024.sa.base.module.support.loginlog.LoginLogResultEnum;
import net.lab1024.sa.base.module.support.loginlog.LoginLogService;
import net.lab1024.sa.base.module.support.loginlog.domain.LoginLogVO;
import net.lab1024.sa.base.module.support.mail.service.MailService;
import net.lab1024.sa.foundation.apiencrypt.service.ApiEncryptService;
import net.lab1024.sa.foundation.cache.CacheService;
import net.lab1024.sa.foundation.captcha.CaptchaException;
import net.lab1024.sa.foundation.captcha.CaptchaService;
import net.lab1024.sa.foundation.captcha.CaptchaVO;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

/**
 * LoginService unit tests
 *
 * <p>Comprehensive test coverage for LoginService with 40+ test cases covering:
 *
 * <ul>
 *   <li>getCaptcha() - 2 tests
 *   <li>login() - 15 tests (MOST CRITICAL - complex validation, 2FA, super password, account
 *       lockout)
 *   <li>getLoginResult() - 4 tests
 *   <li>getLoginEmployee() - 4 tests
 *   <li>getEmployeeIdByLoginId() - 4 tests
 *   <li>logout() - 3 tests
 *   <li>sendEmailCode() - 5 tests
 *   <li>clearLoginEmployeeCache() - 1 test
 * </ul>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@DisplayName("LoginService Tests")
class LoginServiceTest extends BaseUnitTest {

  @InjectMocks private LoginService loginService;

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

  private EmployeeEntity testEmployee;
  private LoginForm loginForm;
  private static final String TEST_IP = "127.0.0.1";
  private static final String TEST_USER_AGENT = "Mozilla/5.0";
  private static final String ENCRYPTED_PASSWORD = "encryptedPassword123";
  private static final String DECRYPTED_PASSWORD = "Password123!";
  private static final String SUPER_PASSWORD = "SuperAdmin888";

  @BeforeEach
  void setUp() {
    testEmployee = EmployeeTestFixture.createEmployee(1001L, "test_user");
    testEmployee.setEmployeeUid("uid_test_user");
    testEmployee.setLoginPwd("$2a$10$hashedPassword");
    testEmployee.setEmail("test@example.com");

    loginForm = new LoginForm();
    loginForm.setLoginName("test_user");
    loginForm.setPassword(ENCRYPTED_PASSWORD);
    loginForm.setLoginDevice(LoginDeviceEnum.PC.getValue());
    loginForm.setCaptchaUuid("captcha-uuid-123");
    loginForm.setCaptchaCode("1234");
  }

  @Nested
  @DisplayName("getCaptcha() Tests")
  class GetCaptchaTests {

    @Test
    @DisplayName("Should return CaptchaVO when captcha generation succeeds")
    void getCaptcha_Success_ReturnsCaptchaVO() {
      // Given
      CaptchaVO expectedCaptcha = new CaptchaVO();
      expectedCaptcha.setCaptchaUuid("uuid-123");
      expectedCaptcha.setCaptchaBase64Image("data:image/png;base64,iVBORw0KG...");
      expectedCaptcha.setExpireSeconds(300L);

      when(captchaService.generateCaptcha()).thenReturn(expectedCaptcha);

      // When
      ResponseDTO<CaptchaVO> response = loginService.getCaptcha();

      // Then
      assertOk(response);
      assertNotNull(response.getData());
      assertEquals("uuid-123", response.getData().getCaptchaUuid());
      verify(captchaService, times(1)).generateCaptcha();
    }

    @Test
    @DisplayName("Should propagate exception when captcha service fails")
    void getCaptcha_ServiceFailure_ThrowsException() {
      // Given
      when(captchaService.generateCaptcha())
          .thenThrow(new RuntimeException("Redis connection failed"));

      // When/Then
      assertThrows(RuntimeException.class, () -> loginService.getCaptcha());
    }
  }

  @Nested
  @DisplayName("login() Tests - CRITICAL Business Logic")
  class LoginTests {

    @BeforeEach
    void setUpMocks() {
      // Default happy path setup
      when(employeeDao.getByLoginName("test_user", false)).thenReturn(testEmployee);
      when(apiEncryptService.decrypt(ENCRYPTED_PASSWORD)).thenReturn(DECRYPTED_PASSWORD);
      when(configService.getConfigValue(ConfigKeyEnum.SUPER_PASSWORD)).thenReturn(SUPER_PASSWORD);
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(false);

      LoginFailEntity loginFailEntity = new LoginFailEntity();
      loginFailEntity.setLoginFailCount(0);
      when(securityLoginService.checkLogin(anyLong(), any(UserTypeEnum.class)))
          .thenReturn(ResponseDTO.ok(loginFailEntity));

      String saltPassword =
          DECRYPTED_PASSWORD
              + "_"
              + testEmployee.getEmployeeUid().toUpperCase()
              + "_"
              + testEmployee.getEmployeeUid().toLowerCase();
      when(protectPasswordService.matchesPwd(saltPassword, testEmployee.getLoginPwd()))
          .thenReturn(true);

      RequestEmployee requestEmployee = new RequestEmployee();
      requestEmployee.setEmployeeId(testEmployee.getEmployeeId());
      requestEmployee.setAdministratorFlag(false);
      when(loginManager.loadLoginInfo(any(EmployeeEntity.class))).thenReturn(requestEmployee);

      when(roleEmployeeDao.selectRoleByEmployeeId(anyLong())).thenReturn(Collections.emptyList());
      when(loginLogService.queryLastByUserId(
              anyLong(), any(UserTypeEnum.class), any(LoginLogResultEnum.class)))
          .thenReturn(null);
      when(protectPasswordService.checkNeedChangePassword(anyInt(), anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_ValidCredentials_ReturnsSuccess() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock
            .when(() -> StpUtil.login(anyString(), anyString()))
            .thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("token-123");
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(anyString())).thenReturn("1:1001");

        ResponseDTO<LoginResultVO> response =
            loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

        // Then
        assertOk(response);
        assertNotNull(response.getData());
        assertEquals("token-123", response.getData().getToken());
        verify(loginLogService, times(1))
            .log(
                argThat(
                    log ->
                        log.getLoginResult().equals(LoginLogResultEnum.LOGIN_SUCCESS.getValue())));
        verify(securityLoginService, times(1))
            .removeLoginFail(testEmployee.getEmployeeId(), UserTypeEnum.ADMIN_EMPLOYEE);
      }
    }

    @Test
    @DisplayName("Should return error when login device is invalid")
    void login_InvalidDevice_ReturnsError() {
      // Given
      loginForm.setLoginDevice(999);

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "登录设备暂不支持");
    }

    @Test
    @DisplayName("Should return error when captcha validation fails")
    void login_CaptchaValidationFailure_ReturnsError() {
      // Given
      doThrow(new CaptchaException("验证码错误"))
          .when(captchaService)
          .checkCaptcha(any(LoginForm.class));

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "验证码错误");
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void login_EmployeeNotExist_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(employeeDao.getByLoginName("test_user", false)).thenReturn(null);

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "登录名或密码错误");
    }

    @Test
    @DisplayName("Should return error when account is deleted")
    void login_DeletedAccount_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      testEmployee.setDeletedFlag(true);

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "账号已删除");
      verify(loginLogService, times(1))
          .log(
              argThat(
                  log ->
                      log.getLoginResult().equals(LoginLogResultEnum.LOGIN_FAIL.getValue())
                          && log.getRemark().equals("账号已删除")));
    }

    @Test
    @DisplayName("Should return error when account is disabled")
    void login_DisabledAccount_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      testEmployee.setDisabledFlag(true);

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "账号已禁用");
      verify(loginLogService, times(1))
          .log(
              argThat(
                  log ->
                      log.getLoginResult().equals(LoginLogResultEnum.LOGIN_FAIL.getValue())
                          && log.getRemark().equals("账号已禁用")));
    }

    @Test
    @DisplayName("Should return error when password is incorrect")
    void login_WrongPassword_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      String saltPassword =
          DECRYPTED_PASSWORD
              + "_"
              + testEmployee.getEmployeeUid().toUpperCase()
              + "_"
              + testEmployee.getEmployeeUid().toLowerCase();
      when(protectPasswordService.matchesPwd(saltPassword, testEmployee.getLoginPwd()))
          .thenReturn(false);
      when(securityLoginService.recordLoginFail(
              anyLong(), any(UserTypeEnum.class), anyString(), any()))
          .thenReturn(null);

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "登录名或密码错误");
      verify(loginLogService, times(1))
          .log(
              argThat(
                  log ->
                      log.getLoginResult().equals(LoginLogResultEnum.LOGIN_FAIL.getValue())
                          && log.getRemark().equals("密码错误")));
      verify(securityLoginService, times(1))
          .recordLoginFail(
              eq(testEmployee.getEmployeeId()),
              eq(UserTypeEnum.ADMIN_EMPLOYEE),
              anyString(),
              any());
    }

    @Test
    @DisplayName("Should login successfully with super password")
    void login_SuperPassword_ReturnsSuccess() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(apiEncryptService.decrypt(ENCRYPTED_PASSWORD)).thenReturn(SUPER_PASSWORD);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock
            .when(() -> StpUtil.login(argThat(id -> ((String) id).startsWith("S:")), eq(1800)))
            .thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("super-token-123");
        stpUtilMock
            .when(() -> StpUtil.getLoginIdByToken(anyString()))
            .thenReturn("S:uuid-abc:1001");

        ResponseDTO<LoginResultVO> response =
            loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

        // Then
        assertOk(response);
        assertNotNull(response.getData());
        assertFalse(
            response.getData().getNeedUpdatePwdFlag(),
            "Super password login should not require password change");
        verify(loginLogService, times(1))
            .log(
                argThat(
                    log ->
                        log.getLoginResult().equals(LoginLogResultEnum.LOGIN_SUCCESS.getValue())
                            && log.getRemark().equals("万能密码登录")));
        // Super password should not trigger login fail tracking
        verify(securityLoginService, never()).checkLogin(anyLong(), any(UserTypeEnum.class));
      }
    }

    @Test
    @DisplayName("Should return error when 2FA email code is required but missing")
    void login_TwoFactorEnabled_EmailCodeMissing_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      loginForm.setEmailCode(null);

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "请输入邮箱验证码");
    }

    @Test
    @DisplayName("Should return error when email code is invalid")
    void login_TwoFactorEnabled_EmailCodeInvalid_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      loginForm.setEmailCode("9999");
      when(cacheService.get(anyString(), anyString(), eq(String.class)))
          .thenReturn(Optional.of("1234_" + System.currentTimeMillis()));

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "邮箱验证码错误");
    }

    @Test
    @DisplayName("Should login successfully when email code is valid")
    void login_TwoFactorEnabled_EmailCodeValid_ReturnsSuccess() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      loginForm.setEmailCode("1234");
      when(cacheService.get(anyString(), anyString(), eq(String.class)))
          .thenReturn(Optional.of("1234_" + System.currentTimeMillis()));

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock
            .when(() -> StpUtil.login(anyString(), anyString()))
            .thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("token-123");
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(anyString())).thenReturn("1:1001");

        ResponseDTO<LoginResultVO> response =
            loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

        // Then
        assertOk(response);
        verify(cacheService, times(1)).remove(anyString(), anyString());
      }
    }

    @Test
    @DisplayName("Should return error when account is locked due to login failures")
    void login_AccountLocked_ReturnsError() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(securityLoginService.checkLogin(anyLong(), any(UserTypeEnum.class)))
          .thenReturn(ResponseDTO.error(UserErrorCode.LOGIN_FAIL_LOCK, "登录连续失败已经被锁定"));

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.LOGIN_FAIL_LOCK);
      assertErrorContains(response, "登录连续失败已经被锁定");
    }

    @Test
    @DisplayName("Should return error when password decryption fails")
    void login_PasswordDecryptionFailure_ThrowsException() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      when(apiEncryptService.decrypt(ENCRYPTED_PASSWORD))
          .thenThrow(new RuntimeException("Decryption failed"));

      // When/Then
      assertThrows(
          RuntimeException.class, () -> loginService.login(loginForm, TEST_IP, TEST_USER_AGENT));
    }

    @Test
    @DisplayName("Should increment login fail count on password error")
    void login_PasswordError_IncrementsFailCount() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));
      String saltPassword =
          DECRYPTED_PASSWORD
              + "_"
              + testEmployee.getEmployeeUid().toUpperCase()
              + "_"
              + testEmployee.getEmployeeUid().toLowerCase();
      when(protectPasswordService.matchesPwd(saltPassword, testEmployee.getLoginPwd()))
          .thenReturn(false);
      when(securityLoginService.recordLoginFail(
              anyLong(), any(UserTypeEnum.class), anyString(), any()))
          .thenReturn("登录失败3次，再失败2次将锁定账号");

      // When
      ResponseDTO<LoginResultVO> response = loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

      // Then
      assertError(response, UserErrorCode.LOGIN_FAIL_WILL_LOCK);
      assertErrorContains(response, "登录失败3次");
      verify(securityLoginService, times(1))
          .recordLoginFail(
              eq(testEmployee.getEmployeeId()), any(UserTypeEnum.class), anyString(), any());
    }

    @Test
    @DisplayName("Should create login success log on successful login")
    void login_Success_CreatesSuccessLog() {
      // Given
      doNothing().when(captchaService).checkCaptcha(any(LoginForm.class));

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock
            .when(() -> StpUtil.login(anyString(), anyString()))
            .thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("token-123");
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(anyString())).thenReturn("1:1001");

        loginService.login(loginForm, TEST_IP, TEST_USER_AGENT);

        // Then
        verify(loginLogService, times(1))
            .log(
                argThat(
                    log ->
                        log.getUserId().equals(testEmployee.getEmployeeId())
                            && log.getLoginResult()
                                .equals(LoginLogResultEnum.LOGIN_SUCCESS.getValue())
                            && log.getLoginIp().equals(TEST_IP)
                            && log.getUserAgent().equals(TEST_USER_AGENT)));
      }
    }
  }

  @Nested
  @DisplayName("getLoginResult() Tests")
  class GetLoginResultTests {

    private RequestEmployee requestEmployee;
    private String testToken = "test-token-123";

    @BeforeEach
    void setUpRequestEmployee() {
      requestEmployee = new RequestEmployee();
      requestEmployee.setEmployeeId(1001L);
      requestEmployee.setLoginName("test_user");
      requestEmployee.setActualName("Test Employee");
      requestEmployee.setAdministratorFlag(false);
      requestEmployee.setUserType(UserTypeEnum.ADMIN_EMPLOYEE);
    }

    @Test
    @DisplayName("Should return complete login result with menu list and last login info")
    void getLoginResult_CompleteData_ReturnsFullResult() {
      // Given
      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      when(roleEmployeeDao.selectRoleByEmployeeId(1001L)).thenReturn(List.of(role));

      MenuEntity menu = new MenuEntity();
      menu.setMenuId(1L);
      menu.setMenuName("Dashboard");
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), eq(false))).thenReturn(List.of(menu));

      LoginLogVO lastLoginLog = new LoginLogVO();
      lastLoginLog.setLoginIp("192.168.1.100");
      lastLoginLog.setLoginIpRegion("Beijing");
      lastLoginLog.setCreateTime(LocalDateTime.now().minusDays(1));
      lastLoginLog.setUserAgent("Chrome/100");
      when(loginLogService.queryLastByUserId(
              1001L, UserTypeEnum.ADMIN_EMPLOYEE, LoginLogResultEnum.LOGIN_SUCCESS))
          .thenReturn(lastLoginLog);

      when(protectPasswordService.checkNeedChangePassword(anyInt(), anyLong())).thenReturn(false);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(testToken)).thenReturn("1:1001");

        LoginResultVO result = loginService.getLoginResult(requestEmployee, testToken);

        // Then
        assertNotNull(result);
        assertEquals("test_user", result.getLoginName());
        assertNotNull(result.getMenuList());
        assertEquals(1, result.getMenuList().size());
        assertEquals("192.168.1.100", result.getLastLoginIp());
        assertEquals("Beijing", result.getLastLoginIpRegion());
        assertNotNull(result.getLastLoginTime());
        assertFalse(result.getNeedUpdatePwdFlag());
      }
    }

    @Test
    @DisplayName("Should set needUpdatePwdFlag to false for super password login")
    void getLoginResult_SuperPasswordLogin_NoPasswordChangeRequired() {
      // Given
      when(roleEmployeeDao.selectRoleByEmployeeId(anyLong())).thenReturn(Collections.emptyList());
      when(protectPasswordService.checkNeedChangePassword(anyInt(), anyLong())).thenReturn(true);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(testToken)).thenReturn("S:uuid-abc:1001");

        LoginResultVO result = loginService.getLoginResult(requestEmployee, testToken);

        // Then
        assertFalse(
            result.getNeedUpdatePwdFlag(),
            "Super password login should skip password change requirement");
      }
    }

    @Test
    @DisplayName("Should set needUpdatePwdFlag to true when password change is required")
    void getLoginResult_PasswordChangeRequired_FlagSetTrue() {
      // Given
      when(roleEmployeeDao.selectRoleByEmployeeId(anyLong())).thenReturn(Collections.emptyList());
      when(protectPasswordService.checkNeedChangePassword(anyInt(), anyLong())).thenReturn(true);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(testToken)).thenReturn("1:1001");

        LoginResultVO result = loginService.getLoginResult(requestEmployee, testToken);

        // Then
        assertTrue(result.getNeedUpdatePwdFlag());
      }
    }

    @Test
    @DisplayName("Should handle empty roles gracefully")
    void getLoginResult_NoRoles_ReturnsEmptyMenuList() {
      // Given
      when(roleEmployeeDao.selectRoleByEmployeeId(anyLong())).thenReturn(Collections.emptyList());
      when(protectPasswordService.checkNeedChangePassword(anyInt(), anyLong())).thenReturn(false);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(() -> StpUtil.getLoginIdByToken(testToken)).thenReturn("1:1001");

        LoginResultVO result = loginService.getLoginResult(requestEmployee, testToken);

        // Then
        assertNotNull(result.getMenuList());
        assertTrue(result.getMenuList().isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("getLoginEmployee() Tests")
  class GetLoginEmployeeTests {

    @Mock private HttpServletRequest request;

    @Test
    @DisplayName("Should return RequestEmployee for valid loginId")
    void getLoginEmployee_ValidLoginId_ReturnsRequestEmployee() {
      // Given
      String loginId = "1:1001";
      RequestEmployee expected = new RequestEmployee();
      expected.setEmployeeId(1001L);
      when(loginManager.getRequestEmployee(1001L)).thenReturn(expected);
      when(request.getHeader("user-agent")).thenReturn(TEST_USER_AGENT);

      // When
      RequestEmployee result = loginService.getLoginEmployee(loginId, request);

      // Then
      assertNotNull(result);
      assertEquals(1001L, result.getEmployeeId());
      assertEquals(TEST_USER_AGENT, result.getUserAgent());
      verify(loginManager, times(1)).getRequestEmployee(1001L);
    }

    @Test
    @DisplayName("Should return null when loginId is null")
    void getLoginEmployee_NullLoginId_ReturnsNull() {
      // When
      RequestEmployee result = loginService.getLoginEmployee(null, request);

      // Then
      assertNull(result);
      verify(loginManager, never()).getRequestEmployee(anyLong());
    }

    @Test
    @DisplayName("Should return null when employeeId cannot be parsed")
    void getLoginEmployee_InvalidLoginId_ReturnsNull() {
      // When
      RequestEmployee result = loginService.getLoginEmployee("invalid-format", request);

      // Then
      assertNull(result);
    }

    @Test
    @DisplayName("Should parse super password loginId correctly")
    void getLoginEmployee_SuperPasswordLoginId_ReturnsEmployee() {
      // Given
      String superLoginId = "S:uuid-abc-def:1001";
      RequestEmployee expected = new RequestEmployee();
      expected.setEmployeeId(1001L);
      when(loginManager.getRequestEmployee(1001L)).thenReturn(expected);
      when(request.getHeader("user-agent")).thenReturn(TEST_USER_AGENT);

      // When
      RequestEmployee result = loginService.getLoginEmployee(superLoginId, request);

      // Then
      assertNotNull(result);
      assertEquals(1001L, result.getEmployeeId());
    }
  }

  @Nested
  @DisplayName("getEmployeeIdByLoginId() Tests")
  class GetEmployeeIdByLoginIdTests {

    @Test
    @DisplayName("Should extract employee ID from normal loginId")
    void getEmployeeIdByLoginId_NormalFormat_ReturnsId() {
      // Given
      String loginId = "1:1001";

      // When
      Long employeeId = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertEquals(1001L, employeeId);
    }

    @Test
    @DisplayName("Should extract employee ID from super password loginId")
    void getEmployeeIdByLoginId_SuperPasswordFormat_ReturnsId() {
      // Given
      String loginId = "S:uuid-abc-def-123:1001";

      // When
      Long employeeId = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertEquals(1001L, employeeId);
    }

    @Test
    @DisplayName("Should return null for invalid loginId format")
    void getEmployeeIdByLoginId_InvalidFormat_ReturnsNull() {
      // Given
      String loginId = "invalid-format";

      // When
      Long employeeId = loginService.getEmployeeIdByLoginId(loginId);

      // Then
      assertNull(employeeId);
    }

    @Test
    @DisplayName("Should return null when loginId is null")
    void getEmployeeIdByLoginId_Null_ReturnsNull() {
      // When
      Long employeeId = loginService.getEmployeeIdByLoginId(null);

      // Then
      assertNull(employeeId);
    }
  }

  @Nested
  @DisplayName("logout() Tests")
  class LogoutTests {

    @Test
    @DisplayName("Should logout successfully and clear cache")
    void logout_Success_ClearsCache() {
      // Given
      RequestEmployee requestUser = new RequestEmployee();
      requestUser.setEmployeeId(1001L);
      requestUser.setActualName("Test Employee");

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

        ResponseDTO<String> response = loginService.logout(requestUser);

        // Then
        assertOk(response);
        stpUtilMock.verify(StpUtil::logout, times(1));
        verify(loginManager, times(1)).clearUserPermission(1001L);
        verify(loginManager, times(1)).clearUserLoginInfo(1001L);
      }
    }

    @Test
    @DisplayName("Should create logout log when logging out")
    void logout_Success_CreatesLogoutLog() {
      // Given
      RequestEmployee requestUser = new RequestEmployee();
      requestUser.setEmployeeId(1001L);
      requestUser.setActualName("Test Employee");
      requestUser.setUserAgent(TEST_USER_AGENT);
      requestUser.setIp(TEST_IP);
      requestUser.setUserType(UserTypeEnum.ADMIN_EMPLOYEE);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

        loginService.logout(requestUser);

        // Then
        verify(loginLogService, times(1))
            .log(
                argThat(
                    log ->
                        log.getUserId().equals(1001L)
                            && log.getLoginResult().equals(LoginLogResultEnum.LOGIN_OUT.getValue())
                            && log.getLoginIp().equals(TEST_IP)
                            && log.getUserAgent().equals(TEST_USER_AGENT)));
      }
    }

    @Test
    @DisplayName("Should call loginManager to clear cache on logout")
    void logout_CallsLoginManager_ClearCache() {
      // Given
      RequestEmployee requestUser = new RequestEmployee();
      requestUser.setEmployeeId(1001L);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

        loginService.logout(requestUser);

        // Then
        verify(loginManager, times(1)).clearUserPermission(1001L);
        verify(loginManager, times(1)).clearUserLoginInfo(1001L);
      }
    }
  }

  @Nested
  @DisplayName("sendEmailCode() Tests")
  class SendEmailCodeTests {

    @Test
    @DisplayName("Should send email code successfully when 2FA is enabled")
    void sendEmailCode_TwoFactorEnabled_SendsEmail() {
      // Given
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName("test_user", false)).thenReturn(testEmployee);
      when(cacheService.get(anyString(), anyString(), eq(String.class)))
          .thenReturn(Optional.empty());
      when(mailService.sendMail(any(), any(), anyList())).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> response = loginService.sendEmailCode("test_user");

      // Then
      assertOk(response);
      verify(cacheService, times(1)).put(anyString(), anyString(), anyString(), eq(300), any());
      verify(mailService, times(1)).sendMail(any(), any(), anyList());
    }

    @Test
    @DisplayName("Should return error when 2FA is disabled")
    void sendEmailCode_TwoFactorDisabled_ReturnsError() {
      // Given
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(false);

      // When
      ResponseDTO<String> response = loginService.sendEmailCode("test_user");

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "无需使用邮箱验证码");
    }

    @Test
    @DisplayName(
        "Should return OK when user does not exist (security - don't reveal user existence)")
    void sendEmailCode_UserNotExist_ReturnsOk() {
      // Given
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName("nonexistent_user", false)).thenReturn(null);

      // When
      ResponseDTO<String> response = loginService.sendEmailCode("nonexistent_user");

      // Then
      assertOk(response);
      verify(mailService, never()).sendMail(any(), any(), anyList());
    }

    @Test
    @DisplayName("Should return error when rate limited (< 60s since last send)")
    void sendEmailCode_RateLimited_ReturnsError() {
      // Given
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      when(employeeDao.getByLoginName("test_user", false)).thenReturn(testEmployee);
      long recentTimestamp = System.currentTimeMillis() - 30000; // 30 seconds ago
      when(cacheService.get(anyString(), anyString(), eq(String.class)))
          .thenReturn(Optional.of("1234_" + recentTimestamp));

      // When
      ResponseDTO<String> response = loginService.sendEmailCode("test_user");

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "一分钟内请勿重复发送");
      verify(mailService, never()).sendMail(any(), any(), anyList());
    }

    @Test
    @DisplayName("Should return error when employee has no email configured")
    void sendEmailCode_NoEmail_ReturnsError() {
      // Given
      when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
      testEmployee.setEmail(null);
      when(employeeDao.getByLoginName("test_user", false)).thenReturn(testEmployee);

      // When
      ResponseDTO<String> response = loginService.sendEmailCode("test_user");

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "暂未配置邮箱地址");
    }
  }

  @Nested
  @DisplayName("clearLoginEmployeeCache() Tests")
  class ClearLoginEmployeeCacheTests {

    @Test
    @DisplayName("Should delegate to LoginManager to clear user cache")
    void clearLoginEmployeeCache_DelegatesToLoginManager() {
      // When
      loginService.clearLoginEmployeeCache(1001L);

      // Then
      verify(loginManager, times(1)).clearUserPermission(1001L);
      verify(loginManager, times(1)).clearUserLoginInfo(1001L);
    }
  }

  @Nested
  @DisplayName("StpInterface Implementation Tests")
  class StpInterfaceTests {

    @Test
    @DisplayName("getPermissionList() should return user permissions")
    void getPermissionList_ValidLoginId_ReturnsPermissions() {
      // Given
      UserPermission userPermission = new UserPermission();
      userPermission.setPermissionList(List.of("employee:add", "employee:update"));
      when(loginManager.getUserPermission(1001L)).thenReturn(userPermission);

      // When
      List<String> permissions = loginService.getPermissionList("1:1001", "admin");

      // Then
      assertEquals(2, permissions.size());
      assertTrue(permissions.contains("employee:add"));
      assertTrue(permissions.contains("employee:update"));
    }

    @Test
    @DisplayName("getRoleList() should return user roles")
    void getRoleList_ValidLoginId_ReturnsRoles() {
      // Given
      UserPermission userPermission = new UserPermission();
      userPermission.setRoleList(List.of("admin", "user"));
      when(loginManager.getUserPermission(1001L)).thenReturn(userPermission);

      // When
      List<String> roles = loginService.getRoleList("1:1001", "admin");

      // Then
      assertEquals(2, roles.size());
      assertTrue(roles.contains("admin"));
      assertTrue(roles.contains("user"));
    }
  }
}
