package net.lab1024.sa.common.core.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Page Parameter Bridge Class (DEPRECATED)
 *
 * @deprecated This bridge class is deprecated and will be removed in v4.0.0. Please migrate to
 *     {@link net.lab1024.sa.foundation.domain.request.PageParam} immediately.
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
 * import net.lab1024.sa.common.core.domain.PageParam;
 *
 * // NEW (required for v4.0.0+)
 * import net.lab1024.sa.foundation.domain.request.PageParam;
 *
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since 3.6.0
 * @see net.lab1024.sa.foundation.domain.request.PageParam New PageParam location
 */
@Slf4j
@Deprecated(since = "3.6.0", forRemoval = true)
public class PageParam extends net.lab1024.sa.foundation.domain.request.PageParam {

  private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

  static {
    if (WARNING_EMITTED.compareAndSet(false, true)) {
      log.warn(
          """

          ╔════════════════════════════════════════════════════════════════════════════╗
          ║ ⚠️  DEPRECATION WARNING - Bridge Class In Use                             ║
          ╠════════════════════════════════════════════════════════════════════════════╣
          ║ Class: net.lab1024.sa.common.core.domain.PageParam                        ║
          ║ Use:   net.lab1024.sa.foundation.domain.request.PageParam                 ║
          ║                                                                            ║
          ║ 📅 Removal: v4.0.0 (Q4 2026) | 🔧 Tool: ./gradlew migrateToFoundation    ║
          ╚════════════════════════════════════════════════════════════════════════════╝
          """);
    }
  }
}
