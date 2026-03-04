package net.lab1024.sa.system.mfa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.system.mfa.controller.MfaController;
import net.lab1024.sa.system.mfa.domain.form.MfaEnableForm;
import net.lab1024.sa.system.mfa.domain.form.MfaVerifyForm;
import net.lab1024.sa.system.mfa.domain.vo.MfaSetupInitVO;
import net.lab1024.sa.system.mfa.domain.vo.MfaStatusVO;
import net.lab1024.sa.system.mfa.service.MfaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MFA Controller 整合測試
 *
 * <p>測試 MFA Controller 的 8 個 API 端點，驗證請求/響應、錯誤處理、權限檢查。
 *
 * <p>使用 MockMvc 進行 HTTP 層測試，Service 層使用 @MockBean 模擬。
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Tag("integration")
@WebMvcTest(MfaController.class)
@DisplayName("MFA Controller 整合測試")
class MfaControllerIntegrationTest {

  /**
   * Minimal Spring Boot configuration for @WebMvcTest.
   *
   * <p>This is required because the test is in a standalone module without a main application
   * class.
   */
  @SpringBootApplication
  static class TestConfig {}

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private MfaService mfaService;

  private static final Long TEST_EMPLOYEE_ID = 100L;
  private static final String TEST_IP_ADDRESS = "192.168.1.1";
  private static final String TEST_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

  /**
   * Create test RequestUser for SmartRequestUtil mocking.
   *
   * <p>Uses lenient() to avoid UnfinishedStubbingException when not all methods are called.
   *
   * @return Mocked RequestUser
   */
  private RequestUser createTestRequestUser() {
    RequestUser requestUser = mock(RequestUser.class);
    lenient().when(requestUser.getUserId()).thenReturn(TEST_EMPLOYEE_ID);
    lenient().when(requestUser.getIp()).thenReturn(TEST_IP_ADDRESS);
    lenient().when(requestUser.getUserAgent()).thenReturn(TEST_USER_AGENT);
    return requestUser;
  }

  @BeforeEach
  void setUp() {
    reset(mfaService);
  }

  @Nested
  @DisplayName("場景1: MFA 設定流程（init → enable → 生成備份碼）")
  class MfaSetupFlowTests {

    @Test
    @DisplayName("1.1 POST /mfa/setup/init - 初始化 MFA 設定（生成 QR 碼）")
    void testInitSetup_Success() throws Exception {
      // Given
      MfaSetupInitVO initVO = new MfaSetupInitVO();
      initVO.setSecret("JBSWY3DPEHPK3PXP");
      initVO.setQrCodeUrl(
          "otpauth://totp/SmartAdmin:test@example.com?secret=JBSWY3DPEHPK3PXP&issuer=SmartAdmin");
      initVO.setQrCodeDataUrl("data:image/png;base64,iVBORw0KGgo...");
      initVO.setAccountName("test@example.com");
      initVO.setIssuer("SmartAdmin");

      when(mfaService.initSetup(TEST_EMPLOYEE_ID)).thenReturn(ResponseDTO.ok(initVO));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);

        mockMvc
            .perform(post("/mfa/setup/init").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.secret").value("JBSWY3DPEHPK3PXP"))
            .andExpect(jsonPath("$.data.qrCodeUrl").exists())
            .andExpect(jsonPath("$.data.qrCodeDataUrl").exists())
            .andExpect(jsonPath("$.data.accountName").value("test@example.com"))
            .andExpect(jsonPath("$.data.issuer").value("SmartAdmin"));

        verify(mfaService, times(1)).initSetup(TEST_EMPLOYEE_ID);
      }
    }

    @Test
    @DisplayName("1.2 POST /mfa/setup/enable - 啟用 MFA（驗證 TOTP + 生成備份碼）")
    void testEnableMfa_Success() throws Exception {
      // Given
      MfaEnableForm form = new MfaEnableForm();
      form.setTotpToken("123456");
      form.setTrustDevice(false);

      List<String> backupCodes =
          Arrays.asList("12345678", "87654321", "11111111", "22222222", "33333333");

      MfaSetupInitVO enableVO = new MfaSetupInitVO();
      enableVO.setBackupCodes(backupCodes);
      enableVO.setSecret("JBSWY3DPEHPK3PXP");
      enableVO.setAccountName("test@example.com");

      when(mfaService.enableMfa(any(MfaEnableForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.ok(enableVO));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/setup/enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.backupCodes").isArray())
            .andExpect(jsonPath("$.data.backupCodes.length()").value(5))
            .andExpect(jsonPath("$.data.backupCodes[0]").value("12345678"))
            .andExpect(jsonPath("$.data.secret").value("JBSWY3DPEHPK3PXP"));

        verify(mfaService, times(1))
            .enableMfa(any(MfaEnableForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }

    @Test
    @DisplayName("1.3 POST /mfa/setup/enable - 啟用失敗（TOTP 驗證碼錯誤）")
    void testEnableMfa_FailWithInvalidTotp() throws Exception {
      // Given
      MfaEnableForm form = new MfaEnableForm();
      form.setTotpToken("999999");
      form.setTrustDevice(false);

      when(mfaService.enableMfa(any(MfaEnableForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.userErrorParam("TOTP 驗證碼錯誤"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/setup/enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.msg").value("TOTP 驗證碼錯誤"));

        verify(mfaService, times(1))
            .enableMfa(any(MfaEnableForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }
  }

  @Nested
  @DisplayName("場景2-3: TOTP 驗證（成功 + 失敗）")
  class TotpVerificationTests {

    @Test
    @DisplayName("2.1 POST /mfa/verify - TOTP 驗證成功（6位正確token）")
    void testVerifyMfa_TotpSuccess() throws Exception {
      // Given
      MfaVerifyForm form = new MfaVerifyForm();
      form.setMfaToken("123456");
      form.setTrustDevice(false);

      when(mfaService.verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.ok("MFA 驗證成功"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data").value("MFA 驗證成功"));

        verify(mfaService, times(1))
            .verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }

    @Test
    @DisplayName("3.1 POST /mfa/verify - TOTP 驗證失敗（錯誤token）")
    void testVerifyMfa_TotpFail() throws Exception {
      // Given
      MfaVerifyForm form = new MfaVerifyForm();
      form.setMfaToken("999999");
      form.setTrustDevice(false);

      when(mfaService.verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.userErrorParam("MFA 驗證碼錯誤"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.msg").value("MFA 驗證碼錯誤"));

        verify(mfaService, times(1))
            .verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }
  }

  @Nested
  @DisplayName("場景4-5: 備份碼驗證（成功 + 失敗）")
  class BackupCodeVerificationTests {

    @Test
    @DisplayName("4.1 POST /mfa/verify - 備份碼驗證成功（8位正確code）")
    void testVerifyMfa_BackupCodeSuccess() throws Exception {
      // Given
      MfaVerifyForm form = new MfaVerifyForm();
      form.setMfaToken("12345678");
      form.setTrustDevice(false);

      when(mfaService.verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.ok("MFA 驗證成功"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data").value("MFA 驗證成功"));

        verify(mfaService, times(1))
            .verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }

    @Test
    @DisplayName("5.1 POST /mfa/verify - 備份碼驗證失敗（已使用code）")
    void testVerifyMfa_BackupCodeAlreadyUsed() throws Exception {
      // Given
      MfaVerifyForm form = new MfaVerifyForm();
      form.setMfaToken("12345678");
      form.setTrustDevice(false);

      when(mfaService.verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.userErrorParam("MFA 驗證碼錯誤"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.msg").value("MFA 驗證碼錯誤"));

        verify(mfaService, times(1))
            .verifyMfa(any(MfaVerifyForm.class), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }
  }

  @Nested
  @DisplayName("場景6: 信任設備流程")
  class TrustedDeviceTests {

    @Test
    @DisplayName("6.1 POST /mfa/trusted-devices/add - 添加信任設備")
    void testAddTrustedDevice_Success() throws Exception {
      // Given
      String deviceName = "我的筆記本電腦";

      when(mfaService.addTrustedDevice(
              eq(TEST_EMPLOYEE_ID), eq(deviceName), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.ok("信任設備已添加，30 天內使用此設備登入時無需 MFA 驗證"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/trusted-devices/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("deviceName", deviceName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data").value("信任設備已添加，30 天內使用此設備登入時無需 MFA 驗證"));

        verify(mfaService, times(1))
            .addTrustedDevice(
                eq(TEST_EMPLOYEE_ID), eq(deviceName), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }
  }

  @Nested
  @DisplayName("場景8: MFA 禁用流程（需 TOTP 驗證）")
  class MfaDisableTests {

    @Test
    @DisplayName("8.1 POST /mfa/setup/disable - 禁用 MFA（需 TOTP 驗證）")
    void testDisableMfa_Success() throws Exception {
      // Given
      String totpToken = "123456";

      when(mfaService.disableMfa(
              eq(TEST_EMPLOYEE_ID), eq(totpToken), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.ok("MFA 已成功禁用"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/setup/disable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("totpToken", totpToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data").value("MFA 已成功禁用"));

        verify(mfaService, times(1))
            .disableMfa(
                eq(TEST_EMPLOYEE_ID), eq(totpToken), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }

    @Test
    @DisplayName("8.2 POST /mfa/setup/disable - 禁用失敗（強制MFA角色）")
    void testDisableMfa_FailWithEnforcedRole() throws Exception {
      // Given
      String totpToken = "123456";

      when(mfaService.disableMfa(
              eq(TEST_EMPLOYEE_ID), eq(totpToken), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.userErrorParam("您的角色需要強制啟用 MFA，無法禁用"));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/setup/disable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("totpToken", totpToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.msg").value("您的角色需要強制啟用 MFA，無法禁用"));

        verify(mfaService, times(1))
            .disableMfa(
                eq(TEST_EMPLOYEE_ID), eq(totpToken), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }
  }

  @Nested
  @DisplayName("其他 API 測試")
  class OtherApiTests {

    @Test
    @DisplayName("GET /mfa/status - 查詢 MFA 狀態")
    void testGetStatus_MfaEnabled() throws Exception {
      // Given
      MfaStatusVO statusVO = new MfaStatusVO();
      statusVO.setMfaEnabled(true);
      statusVO.setMfaType("TOTP");
      statusVO.setBackupCodesGenerated(true);
      statusVO.setRemainingBackupCodes(8);
      statusVO.setLastVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
      statusVO.setEnforcedByRole(false);
      statusVO.setNeedRegenerateBackupCodes(false);
      statusVO.setQrCodeConfirmed(true);

      when(mfaService.getStatus(TEST_EMPLOYEE_ID)).thenReturn(ResponseDTO.ok(statusVO));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);

        mockMvc
            .perform(get("/mfa/status").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.mfaEnabled").value(true))
            .andExpect(jsonPath("$.data.mfaType").value("TOTP"))
            .andExpect(jsonPath("$.data.backupCodesGenerated").value(true))
            .andExpect(jsonPath("$.data.remainingBackupCodes").value(8))
            .andExpect(jsonPath("$.data.enforcedByRole").value(false))
            .andExpect(jsonPath("$.data.needRegenerateBackupCodes").value(false))
            .andExpect(jsonPath("$.data.qrCodeConfirmed").value(true));

        verify(mfaService, times(1)).getStatus(TEST_EMPLOYEE_ID);
      }
    }

    @Test
    @DisplayName("GET /mfa/backup-codes/count - 查詢剩餘備份碼數量")
    void testGetBackupCodeCount() throws Exception {
      // Given
      when(mfaService.getBackupCodeCount(TEST_EMPLOYEE_ID)).thenReturn(ResponseDTO.ok(8));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);

        mockMvc
            .perform(get("/mfa/backup-codes/count").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data").value(8));

        verify(mfaService, times(1)).getBackupCodeCount(TEST_EMPLOYEE_ID);
      }
    }

    @Test
    @DisplayName("POST /mfa/backup-codes/regenerate - 重新生成備份碼（需 TOTP 驗證）")
    void testRegenerateBackupCodes_Success() throws Exception {
      // Given
      String totpToken = "123456";

      List<String> newBackupCodes =
          Arrays.asList("11111111", "22222222", "33333333", "44444444", "55555555");

      MfaSetupInitVO initVO = new MfaSetupInitVO();
      initVO.setBackupCodes(newBackupCodes);

      when(mfaService.regenerateBackupCodes(
              eq(TEST_EMPLOYEE_ID), eq(totpToken), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT)))
          .thenReturn(ResponseDTO.ok(initVO));

      // When & Then
      RequestUser testUser = createTestRequestUser();
      try (MockedStatic<SmartRequestUtil> mockedUtil = Mockito.mockStatic(SmartRequestUtil.class)) {
        mockedUtil.when(SmartRequestUtil::getRequestUserId).thenReturn(TEST_EMPLOYEE_ID);
        mockedUtil.when(SmartRequestUtil::getRequestUser).thenReturn(testUser);

        mockMvc
            .perform(
                post("/mfa/backup-codes/regenerate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("totpToken", totpToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.backupCodes").isArray())
            .andExpect(jsonPath("$.data.backupCodes.length()").value(5))
            .andExpect(jsonPath("$.data.backupCodes[0]").value("11111111"));

        verify(mfaService, times(1))
            .regenerateBackupCodes(
                eq(TEST_EMPLOYEE_ID), eq(totpToken), eq(TEST_IP_ADDRESS), eq(TEST_USER_AGENT));
      }
    }
  }
}
