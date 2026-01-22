package net.lab1024.sa.common.core.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.domain.code.ErrorCode;

/**
 * Response DTO Bridge Class (DEPRECATED)
 *
 * @deprecated This bridge class is deprecated and will be removed in v4.0.0. Please migrate to
 *     {@link net.lab1024.sa.foundation.domain.response.ResponseDTO} immediately.
 *     <p><b>⚠️ BREAKING CHANGE ALERT - Migration Required</b>
 *     <ul>
 *       <li><b>Removal Date:</b> v4.0.0 (Q4 2026)
 *       <li><b>Last Compatible Version:</b> v3.9.0 (Q3 2026)
 *       <li><b>Action Required:</b> Update all imports before upgrading to v4.0.0
 *       <li><b>Automated Migration:</b> {@code ./gradlew migrateToFoundation}
 *       <li><b>Migration Guide:</b> docs/migration/foundation-packages.md
 *     </ul>
 *     <p><b>Migration Example:</b>
 *     <pre>{@code
 * // OLD (will break in v4.0.0)
 * import net.lab1024.sa.common.core.domain.ResponseDTO;
 *
 * // NEW (required for v4.0.0+)
 * import net.lab1024.sa.foundation.domain.response.ResponseDTO;
 *
 * }</pre>
 *     <p><b>Timeline:</b>
 *     <ul>
 *       <li>v3.7.0 (Q2 2026): Migration warnings begin (INFO level)
 *       <li>v3.8.0 (Q2 2026): Warnings intensify (WARN level)
 *       <li>v3.9.0 (Q3 2026): Final warning (ERROR level) - last compatible version
 *       <li>v4.0.0 (Q4 2026): Bridge class removed - compilation will fail
 *     </ul>
 *
 * @author SmartAdmin Team
 * @since 3.6.0
 * @see net.lab1024.sa.foundation.domain.response.ResponseDTO New ResponseDTO location
 */
@Slf4j
@Deprecated(since = "3.6.0", forRemoval = true)
public class ResponseDTO<T> extends net.lab1024.sa.foundation.domain.response.ResponseDTO<T> {

  private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

  static {
    // Emit warning once per JVM lifecycle to avoid log flooding
    if (WARNING_EMITTED.compareAndSet(false, true)) {
      log.warn(
          """

          ╔════════════════════════════════════════════════════════════════════════════╗
          ║ ⚠️  DEPRECATION WARNING - Bridge Class In Use                             ║
          ╠════════════════════════════════════════════════════════════════════════════╣
          ║ Class: net.lab1024.sa.common.core.domain.ResponseDTO                      ║
          ║ Use:   net.lab1024.sa.foundation.domain.response.ResponseDTO              ║
          ║                                                                            ║
          ║ 📅 Removal Schedule: v4.0.0 (Q4 2026)                                     ║
          ║ 🔧 Migration Tool:   ./gradlew migrateToFoundation                        ║
          ║ 📖 Migration Guide:  docs/migration/foundation-packages.md                ║
          ║                                                                            ║
          ║ ⚡ ACTION REQUIRED: Update imports before upgrading to v4.0.0             ║
          ╚════════════════════════════════════════════════════════════════════════════╝
          """);
    }
  }

  public ResponseDTO(Integer code, String level, boolean success, String msg, T data) {
    super(code, level, success, msg, data);
  }

  public ResponseDTO(Integer code, String level, boolean success, String msg) {
    super(code, level, success, msg);
  }

  public ResponseDTO(ErrorCode errorCode, boolean success, String msg, T data) {
    super((net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, success, msg, data);
  }

  @SuppressWarnings("PMD.ShortMethodName")
  public static <T> ResponseDTO<T> ok() {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.ok();
  }

  @SuppressWarnings("PMD.ShortMethodName")
  public static <T> ResponseDTO<T> ok(T data) {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.ok(data);
  }

  public static <T> ResponseDTO<T> okMsg(String msg) {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.okMsg(msg);
  }

  public static <T> ResponseDTO<T> userErrorParam() {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.userErrorParam();
  }

  public static <T> ResponseDTO<T> userErrorParam(String msg) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.userErrorParam(msg);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode, boolean success) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, success);
  }

  public static <T> ResponseDTO<T> error(ResponseDTO<?> responseDTO) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.response.ResponseDTO<?>) responseDTO);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode, String msg) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, msg);
  }

  public static <T> ResponseDTO<T> errorData(ErrorCode errorCode, T data) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.errorData(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, data);
  }
}
