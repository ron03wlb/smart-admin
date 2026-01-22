package net.lab1024.sa.common.core.domain;

/**
 * Request User Interface Bridge (DEPRECATED)
 *
 * @deprecated This bridge interface is deprecated and will be removed in v4.0.0. Please migrate to
 *     {@link net.lab1024.sa.foundation.domain.request.RequestUser} immediately.
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
 * import net.lab1024.sa.common.core.domain.RequestUser;
 *
 * // NEW (required for v4.0.0+)
 * import net.lab1024.sa.foundation.domain.request.RequestUser;
 *
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since 3.6.0
 * @see net.lab1024.sa.foundation.domain.request.RequestUser New RequestUser location
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public interface RequestUser extends net.lab1024.sa.foundation.domain.request.RequestUser {}
