package net.lab1024.sa.common.core.code;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * User Error Code Enum Bridge (DEPRECATED)
 *
 * @deprecated This bridge enum is deprecated and will be removed in v4.0.0. Please migrate to
 *     {@link net.lab1024.sa.foundation.domain.code.UserErrorCode} immediately.
 *     <p><b>⚠️ BREAKING CHANGE ALERT - Migration Required</b>
 *     <ul>
 *       <li><b>Removal Date:</b> v4.0.0 (Q4 2026)
 *       <li><b>Last Compatible Version:</b> v3.9.0 (Q3 2026)
 *       <li><b>Automated Migration:</b> {@code ./gradlew migrateToFoundation}
 *       <li><b>Migration Guide:</b> docs/migration/foundation-packages.md
 *     </ul>
 *     <p><b>Migration Example:</b>
 *     <pre>{@code
 * // OLD (will break in v4.0.0)
 * import net.lab1024.sa.common.core.code.UserErrorCode;
 * return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
 *
 * // NEW (required for v4.0.0+)
 * import net.lab1024.sa.foundation.domain.code.UserErrorCode;
 * return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
 *
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since 3.6.0
 * @see net.lab1024.sa.foundation.domain.code.UserErrorCode New UserErrorCode location
 */
@Slf4j
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

  private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

  static {
    if (WARNING_EMITTED.compareAndSet(false, true)) {
      log.warn(
          """

          ╔════════════════════════════════════════════════════════════════════════════╗
          ║ ⚠️  DEPRECATION WARNING - Bridge Class In Use                             ║
          ╠════════════════════════════════════════════════════════════════════════════╣
          ║ Class: net.lab1024.sa.common.core.code.UserErrorCode                      ║
          ║ Use:   net.lab1024.sa.foundation.domain.code.UserErrorCode                ║
          ║                                                                            ║
          ║ 📅 Removal: v4.0.0 (Q4 2026) | 🔧 Tool: ./gradlew migrateToFoundation    ║
          ╚════════════════════════════════════════════════════════════════════════════╝
          """);
    }
  }

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
