package net.lab1024.sa.base.module.support.mail.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.enumeration.BaseEnum;

/**
 * 邮件模板类型
 *
 * @author 1024创新实验室-创始人兼主任:卓大
 * @since 2024/8/5 Copyright <a href="https://1024lab.net">1024创新实验室</a> ，Since 2012
 */
@Getter
@AllArgsConstructor
public enum MailTemplateTypeEnum implements BaseEnum {
  STRING("string", "字符串替代器"),

  FREEMARKER("freemarker", "freemarker模板引擎");

  private String value;

  private String desc;
}
