package net.lab1024.sa.common.repeatsubmit.exception;

/**
 * 重复提交异常
 *
 * <p>当检测到重复提交时抛出此异常，由全局异常处理器统一处理并返回友好的错误信息。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class RepeatSubmitException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** 默认错误消息 */
  private static final String DEFAULT_MESSAGE = "请勿重复提交";

  /** 无参构造函数，使用默认错误消息 */
  public RepeatSubmitException() {
    super(DEFAULT_MESSAGE);
  }

  /**
   * 带消息的构造函数
   *
   * @param message 错误消息
   */
  public RepeatSubmitException(String message) {
    super(message);
  }

  /**
   * 带消息和原因的构造函数
   *
   * @param message 错误消息
   * @param cause 异常原因
   */
  public RepeatSubmitException(String message, Throwable cause) {
    super(message, cause);
  }
}
