package net.lab1024.sa.common.core.code;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum UnexpectedErrorCode implements ErrorCode {
  BUSINESS_HANDING(20001, "呃~ 业务繁忙，请稍后重试"),
  PAY_ORDER_ID_ERROR(20002, "付款单id发生了异常，请联系技术人员排查");

  private final net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode delegate;

  UnexpectedErrorCode(int code, String msg) {
    this.delegate = net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode.valueOf(this.name());
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
