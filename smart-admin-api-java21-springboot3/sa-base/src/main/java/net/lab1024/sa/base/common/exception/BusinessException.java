package net.lab1024.sa.base.common.exception;

import lombok.Getter;
import net.lab1024.sa.base.common.code.ErrorCode;

/**
 * 业务逻辑异常,全局异常拦截后统一返回ResponseCodeConst.SYSTEM_ERROR
 *
 * @author 1024创新实验室: 罗伊
 * @since 2020/8/25 21:57 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Getter
public class BusinessException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private Integer code;

  public BusinessException() {
    // empty
  }

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMsg());
    this.code = errorCode.getCode();
  }

  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }

  public BusinessException(Throwable cause) {
    super(cause);
  }

  public BusinessException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
