package net.lab1024.sa.support.mail.service;

import cn.hutool.core.util.IdUtil;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.SystemEnvironment;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.mail.constant.MailTemplateCodeEnum;
import net.lab1024.sa.support.mail.constant.MailTemplateTypeEnum;
import net.lab1024.sa.support.mail.dao.MailTemplateDao;
import net.lab1024.sa.support.mail.domain.MailTemplateEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 发送邮件：<br>
 * 1、支持直接发送 <br>
 * 2、支持使用邮件模板发送
 *
 * @author 1024创新实验室-创始人兼主任:卓大
 * @since 2024/8/5 Copyright <a href="https://1024lab.net">1024创新实验室</a> ，Since 2012
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.LongVariable")
public class MailService {

  private final JavaMailSender javaMailSender;

  private final MailTemplateDao mailTemplateDao;

  private final SystemEnvironment systemEnvironment;

  @Value("${spring.mail.username}")
  private String clientMail;

  /** 使用模板发送邮件 */
  public ResponseDTO<String> sendMail(
      final MailTemplateCodeEnum templateCode,
      final Map<String, Object> templateParamsMap,
      final List<String> receiverUserList,
      final List<File> fileList) {

    final MailTemplateEntity mailTemplateEntity =
        mailTemplateDao.selectById(templateCode.name().toLowerCase(java.util.Locale.ROOT));
    if (mailTemplateEntity == null) {
      return ResponseDTO.userErrorParam("模版不存在");
    }

    if (mailTemplateEntity.getDisableFlag()) {
      return ResponseDTO.userErrorParam("模版已禁用，无法发送");
    }

    final String content;
    if (MailTemplateTypeEnum.FREEMARKER
        .name()
        .equalsIgnoreCase(mailTemplateEntity.getTemplateType().trim())) {
      content =
          freemarkerResolverContent(mailTemplateEntity.getTemplateContent(), templateParamsMap);
    } else if (MailTemplateTypeEnum.STRING
        .name()
        .equalsIgnoreCase(mailTemplateEntity.getTemplateType().trim())) {
      content = stringResolverContent(mailTemplateEntity.getTemplateContent(), templateParamsMap);
    } else {
      return ResponseDTO.userErrorParam("模版类型不存在");
    }

    try {

      this.sendMail(
          mailTemplateEntity.getTemplateSubject(), content, fileList, receiverUserList, true);

    } catch (MessagingException e) {
      if (log.isErrorEnabled()) {
        log.error("邮件发送失败", e);
      }
      return ResponseDTO.userErrorParam("邮件发送失败");
    }
    return ResponseDTO.ok();
  }

  /** 使用模板发送邮件 */
  public ResponseDTO<String> sendMail(
      final MailTemplateCodeEnum templateCode,
      final Map<String, Object> templateParamsMap,
      final List<String> receiverUserList) {
    return this.sendMail(templateCode, templateParamsMap, receiverUserList, null);
  }

  /**
   * 发送邮件
   *
   * @param subject 主题
   * @param content 内容
   * @param fileList 文件
   * @param receiverUserList 接收方
   * @throws MessagingException
   */
  public void sendMail(
      final String subject,
      final String content,
      final List<File> fileList,
      final List<String> receiverUserList,
      final boolean isHtml)
      throws MessagingException {

    if (CollectionUtils.isEmpty(receiverUserList)) {
      throw new RuntimeException("接收方不能为空");
    }

    if (StringUtils.isBlank(content)) {
      throw new RuntimeException("邮件内容不能为空");
    }

    String actualSubject = subject;
    if (!systemEnvironment.isProd()) {
      actualSubject = "(测试)" + subject;
    }

    final MimeMessage mimeMessage = javaMailSender.createMimeMessage();

    // 是否为多文件上传
    final boolean multiparty = !CollectionUtils.isEmpty(fileList);
    final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multiparty);
    helper.setFrom(clientMail);
    helper.setTo(receiverUserList.toArray(new String[0]));
    helper.setSubject(actualSubject);
    // 发送html格式
    helper.setText(content, isHtml);

    // 附件
    if (multiparty) {
      for (final File file : fileList) {
        helper.addAttachment(file.getName(), file);
      }
    }
    javaMailSender.send(mimeMessage);
  }

  /** 使用字符串生成最终内容 */
  private String stringResolverContent(
      final String stringTemplate, final Map<String, Object> templateParamsMap) {
    final StringSubstitutor stringSubstitutor = new StringSubstitutor(templateParamsMap);
    final String contractHtml = stringSubstitutor.replace(stringTemplate);
    final Document doc = Jsoup.parse(contractHtml);
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    return doc.outerHtml();
  }

  /** 使用 freemarker 生成最终内容 */
  private String freemarkerResolverContent(
      final String htmlTemplate, final Map<String, Object> templateParamsMap) {
    final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
    final StringTemplateLoader stringLoader = new StringTemplateLoader();
    final String templateName = IdUtil.fastSimpleUUID();
    stringLoader.putTemplate(templateName, htmlTemplate);
    configuration.setTemplateLoader(stringLoader);
    try {
      final Template template = configuration.getTemplate(templateName, "utf-8");
      final Writer out = new StringWriter(2048);
      template.process(templateParamsMap, out);
      return out.toString();
    } catch (IOException | TemplateException e) {
      if (log.isErrorEnabled()) {
        log.error("freemarkerResolverContent error: ", e);
      }
    }
    return "";
  }
}
