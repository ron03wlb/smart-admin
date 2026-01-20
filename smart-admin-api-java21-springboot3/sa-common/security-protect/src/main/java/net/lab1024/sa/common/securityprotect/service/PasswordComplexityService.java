package net.lab1024.sa.common.securityprotect.service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 密码复杂度校验服务
 *
 * <p>提供密码复杂度验证和随机密码生成功能。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class PasswordComplexityService {

  /** 密码长度8-20位且包含大小写字母、数字、特殊符号三种及以上组合 */
  public static final String PASSWORD_PATTERN =
      "^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_!@#$%^&*`~()-+=]+$)"
          + "(?![a-z0-9]+$)(?![a-z\\W_!@#$%^&*`~()-+=]+$)"
          + "(?![0-9\\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\\W_!@#$%^&*`~()-+=]*$";

  public static final String PASSWORD_FORMAT_MSG =
      "密码必须为长度8-20位且必须包含大小写字母、数字、特殊符号（如：@#$%^&*()_+-=）等三种字符";

  private static final int PASSWORD_MIN_LENGTH = 8;

  private static final int PASSWORD_MAX_LENGTH = 20;

  /**
   * 校验密码复杂度
   *
   * @param password 待校验的密码
   * @param complexityEnabled 是否开启复杂度校验
   * @return 校验失败时返回错误消息，校验通过返回 empty
   */
  public Optional<String> validateComplexity(String password, boolean complexityEnabled) {
    if (StringUtils.isEmpty(password)) {
      return Optional.of(PASSWORD_FORMAT_MSG);
    }

    // 密码长度必须大于等于8位
    if (password.length() < PASSWORD_MIN_LENGTH) {
      return Optional.of(PASSWORD_FORMAT_MSG);
    }

    // 密码长度必须小于等于20位
    if (password.length() > PASSWORD_MAX_LENGTH) {
      return Optional.of(PASSWORD_FORMAT_MSG);
    }

    // 无需校验密码复杂度
    if (!complexityEnabled) {
      return Optional.empty();
    }

    if (!password.matches(PASSWORD_PATTERN)) {
      return Optional.of(PASSWORD_FORMAT_MSG);
    }

    return Optional.empty();
  }

  /**
   * 随机生成密码
   *
   * @param complexityEnabled 是否开启复杂度校验
   * @return 随机生成的密码
   */
  public String generateRandomPassword(boolean complexityEnabled) {
    // 未开启密码复杂度，则由8位数字构成
    if (!complexityEnabled) {
      return RandomStringUtils.randomNumeric(PASSWORD_MIN_LENGTH);
    }

    // 3位大写字母，2位数字，2位小写字母 + 1位特殊符号
    return RandomStringUtils.randomAlphabetic(3).toUpperCase(java.util.Locale.ROOT)
        + RandomStringUtils.randomNumeric(2)
        + RandomStringUtils.randomAlphabetic(2).toLowerCase(java.util.Locale.ROOT)
        + (ThreadLocalRandom.current().nextBoolean() ? "#" : "@");
  }
}
