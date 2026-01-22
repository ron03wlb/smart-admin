package net.lab1024.sa.common.core.code;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * System Error Code Enum Bridge (DEPRECATED)
 *
 * @deprecated This bridge enum is deprecated and will be removed in v4.0.0. Please migrate to
 *     {@link net.lab1024.sa.foundation.domain.code.SystemErrorCode} immediately.
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
 * import net.lab1024.sa.common.core.code.SystemErrorCode;
 * throw new BusinessException(SystemErrorCode.SYSTEM_ERROR);
 *
 * // NEW (required for v4.0.0+)
 * import net.lab1024.sa.foundation.domain.code.SystemErrorCode;
 * throw new BusinessException(SystemErrorCode.SYSTEM_ERROR);
 *
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since 3.6.0
 * @see net.lab1024.sa.foundation.domain.code.SystemErrorCode New SystemErrorCode location
 */
@Slf4j
@Deprecated(since = "3.6.0", forRemoval = true)
public enum SystemErrorCode implements ErrorCode {
  SYSTEM_ERROR(10001, "系统似乎出现了点小问题");

  private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

  static {
    if (WARNING_EMITTED.compareAndSet(false, true)) {
      log.warn(
          """

          ╔════════════════════════════════════════════════════════════════════════════╗
          ║ ⚠️  DEPRECATION WARNING - Bridge Class In Use                             ║
          ╠════════════════════════════════════════════════════════════════════════════╣
          ║ Class: net.lab1024.sa.common.core.code.SystemErrorCode                    ║
          ║ Use:   net.lab1024.sa.foundation.domain.code.SystemErrorCode              ║
          ║                                                                            ║
          ║ 📅 Removal: v4.0.0 (Q4 2026) | 🔧 Tool: ./gradlew migrateToFoundation    ║
          ╚════════════════════════════════════════════════════════════════════════════╝
          """);
    }
  }

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
