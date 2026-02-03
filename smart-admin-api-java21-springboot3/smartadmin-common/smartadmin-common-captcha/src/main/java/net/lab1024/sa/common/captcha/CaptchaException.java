package net.lab1024.sa.common.captcha;

import java.io.Serial;

/**
 * 验证码异常类
 *
 * <p>用于验证码相关的业务异常，包括验证码过期、验证码错误等场景。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class CaptchaException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * 构造验证码异常
   *
   * @param message 异常信息
   */
  public CaptchaException(String message) {
    super(message);
  }

  /**
   * 构造验证码异常
   *
   * @param message 异常信息
   * @param cause 原始异常
   */
  public CaptchaException(String message, Throwable cause) {
    super(message, cause);
  }
}
