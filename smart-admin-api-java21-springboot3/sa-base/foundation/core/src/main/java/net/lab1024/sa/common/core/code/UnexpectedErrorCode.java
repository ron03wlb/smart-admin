package net.lab1024.sa.common.core.code;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Unexpected Error Code Enum Bridge (DEPRECATED)
 *
 * @deprecated This bridge enum is deprecated and will be removed in v4.0.0. Please migrate to
 *     {@link net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode} immediately.
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
 * import net.lab1024.sa.common.core.code.UnexpectedErrorCode;
 * return ResponseDTO.error(UnexpectedErrorCode.BUSINESS_HANDING);
 *
 * // NEW (required for v4.0.0+)
 * import net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode;
 * return ResponseDTO.error(UnexpectedErrorCode.BUSINESS_HANDING);
 *
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since 3.6.0
 * @see net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode New UnexpectedErrorCode location
 */
@Slf4j
@Deprecated(since = "3.6.0", forRemoval = true)
public enum UnexpectedErrorCode implements ErrorCode {
  BUSINESS_HANDING(20001, "呃~ 业务繁忙，请稍后重试"),
  PAY_ORDER_ID_ERROR(20002, "付款单id发生了异常，请联系技术人员排查");

  private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

  static {
    if (WARNING_EMITTED.compareAndSet(false, true)) {
      log.warn(
          """

          ╔════════════════════════════════════════════════════════════════════════════╗
          ║ ⚠️  DEPRECATION WARNING - Bridge Class In Use                             ║
          ╠════════════════════════════════════════════════════════════════════════════╣
          ║ Class: net.lab1024.sa.common.core.code.UnexpectedErrorCode                ║
          ║ Use:   net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode          ║
          ║                                                                            ║
          ║ 📅 Removal: v4.0.0 (Q4 2026) | 🔧 Tool: ./gradlew migrateToFoundation    ║
          ╚════════════════════════════════════════════════════════════════════════════╝
          """);
    }
  }

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
