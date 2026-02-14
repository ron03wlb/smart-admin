package net.lab1024.sa.support.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.common.core.domain.SystemEnvironment;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.mail.constant.MailTemplateCodeEnum;
import net.lab1024.sa.support.mail.constant.MailTemplateTypeEnum;
import net.lab1024.sa.support.mail.dao.MailTemplateDao;
import net.lab1024.sa.support.mail.domain.MailTemplateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * MailService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>sendMail(templateCode, params, receivers, files) - 模板發送
 *   <li>sendMail(templateCode, params, receivers) - 模板發送（無附件）
 *   <li>sendMail(subject, content, files, receivers, isHtml) - 原始發送
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailService 單元測試")
class MailServiceTest {

  @Mock private JavaMailSender javaMailSender;

  @Mock private MailTemplateDao mailTemplateDao;

  @Mock private SystemEnvironment systemEnvironment;

  @Mock private MimeMessage mimeMessage;

  @InjectMocks private MailService mailService;

  @BeforeEach
  void setUp() {
    // 設置 @Value 注入的私有欄位
    ReflectionTestUtils.setField(mailService, "clientMail", "test@example.com");
  }

  // ==================== sendMail (template) 測試 ====================

  @Nested
  @DisplayName("sendMail 模板發送測試")
  class SendMailTemplateTest {

    @Test
    @DisplayName("模板不存在：應該返回錯誤")
    void shouldReturnErrorWhenTemplateNotExists() {
      // Given
      MailTemplateCodeEnum templateCode = MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE;
      Map<String, Object> params = new HashMap<>();
      List<String> receivers = List.of("test@example.com");

      when(mailTemplateDao.selectById("login_verification_code")).thenReturn(null);

      // When
      ResponseDTO<String> result = mailService.sendMail(templateCode, params, receivers, null);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("模版不存在");
    }

    @Test
    @DisplayName("模板已禁用：應該返回錯誤")
    void shouldReturnErrorWhenTemplateDisabled() {
      // Given
      MailTemplateCodeEnum templateCode = MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE;
      Map<String, Object> params = new HashMap<>();
      List<String> receivers = List.of("test@example.com");

      MailTemplateEntity template = createTestMailTemplate();
      template.setDisableFlag(true);
      when(mailTemplateDao.selectById("login_verification_code")).thenReturn(template);

      // When
      ResponseDTO<String> result = mailService.sendMail(templateCode, params, receivers, null);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("禁用");
    }

    @Test
    @DisplayName("模板類型不存在：應該返回錯誤")
    void shouldReturnErrorWhenTemplateTypeNotExists() {
      // Given
      MailTemplateCodeEnum templateCode = MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE;
      Map<String, Object> params = new HashMap<>();
      List<String> receivers = List.of("test@example.com");

      MailTemplateEntity template = createTestMailTemplate();
      template.setTemplateType("UNKNOWN_TYPE");
      when(mailTemplateDao.selectById("login_verification_code")).thenReturn(template);

      // When
      ResponseDTO<String> result = mailService.sendMail(templateCode, params, receivers, null);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("模版类型不存在");
    }

    @Test
    @DisplayName("STRING 模板類型：應該成功發送")
    void shouldSendWithStringTemplate() throws Exception {
      // Given
      MailTemplateCodeEnum templateCode = MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE;
      Map<String, Object> params = new HashMap<>();
      params.put("code", "123456");
      List<String> receivers = List.of("test@example.com");

      MailTemplateEntity template = createTestMailTemplate();
      template.setTemplateType(MailTemplateTypeEnum.STRING.name());
      template.setTemplateContent("Your verification code is: ${code}");

      when(mailTemplateDao.selectById("login_verification_code")).thenReturn(template);
      when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
      when(systemEnvironment.isProd()).thenReturn(true);

      // When
      ResponseDTO<String> result = mailService.sendMail(templateCode, params, receivers, null);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("無附件版本：應該成功發送")
    void shouldSendWithoutFiles() throws Exception {
      // Given
      MailTemplateCodeEnum templateCode = MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE;
      Map<String, Object> params = new HashMap<>();
      params.put("code", "123456");
      List<String> receivers = List.of("test@example.com");

      MailTemplateEntity template = createTestMailTemplate();
      template.setTemplateType(MailTemplateTypeEnum.STRING.name());
      template.setTemplateContent("Your verification code is: ${code}");

      when(mailTemplateDao.selectById("login_verification_code")).thenReturn(template);
      when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
      when(systemEnvironment.isProd()).thenReturn(true);

      // When
      ResponseDTO<String> result = mailService.sendMail(templateCode, params, receivers);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== sendMail (direct) 測試 ====================

  @Nested
  @DisplayName("sendMail 直接發送測試")
  class SendMailDirectTest {

    @Test
    @DisplayName("接收方為空：應該拋出異常")
    void shouldThrowExceptionWhenReceiversEmpty() {
      // Given
      String subject = "測試主題";
      String content = "測試內容";
      List<String> receivers = List.of();

      // When & Then
      assertThatThrownBy(() -> mailService.sendMail(subject, content, null, receivers, true))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("接收方不能为空");
    }

    @Test
    @DisplayName("內容為空：應該拋出異常")
    void shouldThrowExceptionWhenContentBlank() {
      // Given
      String subject = "測試主題";
      String content = "";
      List<String> receivers = List.of("test@example.com");

      // When & Then
      assertThatThrownBy(() -> mailService.sendMail(subject, content, null, receivers, true))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("邮件内容不能为空");
    }
  }

  // ==================== Helper Methods ====================

  private MailTemplateEntity createTestMailTemplate() {
    MailTemplateEntity entity = new MailTemplateEntity();
    entity.setTemplateCode("login_verification_code");
    entity.setTemplateSubject("驗證碼");
    entity.setTemplateType(MailTemplateTypeEnum.STRING.name());
    entity.setTemplateContent("Your verification code is: ${code}");
    entity.setDisableFlag(false);
    entity.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return entity;
  }
}
