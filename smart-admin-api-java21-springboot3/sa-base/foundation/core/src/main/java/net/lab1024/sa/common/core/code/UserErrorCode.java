package net.lab1024.sa.common.core.code;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.code.UserErrorCode} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public enum UserErrorCode implements ErrorCode {
  PARAM_ERROR(30001, "参数错误"),
  DATA_NOT_EXIST(30002, "左翻右翻，数据竟然找不到了~"),
  ALREADY_EXIST(30003, "数据已存在了呀~"),
  REPEAT_SUBMIT(30004, "亲~您操作的太快了，请稍等下再操作~"),
  NO_PERMISSION(30005, "对不起，您没有权限访问此内容哦~"),
  DEVELOPING(30006, "系統正在紧急开发中，敬请期待~"),
  LOGIN_STATE_INVALID(30007, "您还未登录或登录失效，请重新登录！"),
  USER_STATUS_ERROR(30008, "用户状态异常"),
  FORM_REPEAT_SUBMIT(30009, "请勿重复提交"),
  LOGIN_FAIL_LOCK(30010, "登录连续失败已经被锁定，无法登录"),
  LOGIN_FAIL_WILL_LOCK(30011, "登录连续失败将会锁定提醒"),
  LOGIN_ACTIVE_TIMEOUT(30012, "长时间未操作系统，需要重新登录");

  private final net.lab1024.sa.foundation.domain.code.UserErrorCode delegate;

  UserErrorCode(int code, String msg) {
    this.delegate = net.lab1024.sa.foundation.domain.code.UserErrorCode.valueOf(this.name());
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
