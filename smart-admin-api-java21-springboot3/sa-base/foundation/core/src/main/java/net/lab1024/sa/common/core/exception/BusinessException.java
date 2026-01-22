package net.lab1024.sa.common.core.exception;

import net.lab1024.sa.foundation.domain.code.ErrorCode;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.exception.BusinessException} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public class BusinessException
    extends net.lab1024.sa.foundation.domain.exception.BusinessException {

  public BusinessException() {
    super();
  }

  public BusinessException(ErrorCode errorCode) {
    super((net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode);
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
