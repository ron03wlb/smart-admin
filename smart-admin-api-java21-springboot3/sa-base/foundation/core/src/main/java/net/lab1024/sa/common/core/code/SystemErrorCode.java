package net.lab1024.sa.common.core.code;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.code.SystemErrorCode} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum SystemErrorCode implements ErrorCode {
  SYSTEM_ERROR(10001, "系统似乎出现了点小问题");

  private final net.lab1024.sa.foundation.domain.code.SystemErrorCode delegate;

  SystemErrorCode(int code, String msg) {
    this.delegate = net.lab1024.sa.foundation.domain.code.SystemErrorCode.SYSTEM_ERROR;
  }

  @Override
  public int getCode() {
    return delegate.getCode();
  }

  @Override
  public String getMsg() {
    return delegate.getMsg();
  }

  @Override
  public String getLevel() {
    return delegate.getLevel();
  }
}
