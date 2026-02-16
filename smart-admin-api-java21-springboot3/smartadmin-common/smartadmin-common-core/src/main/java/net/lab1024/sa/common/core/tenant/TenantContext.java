package net.lab1024.sa.common.core.tenant;

import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Tenant context holder using ThreadLocal.
 *
 * <p>Stores the current tenant ID and timezone for the request lifecycle. Set by TenantFilter,
 * consumed by MyBatis-Plus interceptor, Jackson serializer, etc.
 *
 * <p>Located in common-core (not common-tenant) to avoid circular dependencies. Both common-mybatis
 * and common-web depend on common-core.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public class TenantContext {

  private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

  private static final ThreadLocal<String> TIMEZONE = new ThreadLocal<>();

  private TenantContext() {
    // utility class
  }

  public static void setTenantId(Long tenantId) {
    TENANT_ID.set(tenantId);
  }

  public static Long getTenantId() {
    return TENANT_ID.get();
  }

  public static void setTimezone(String timezone) {
    TIMEZONE.set(timezone);
  }

  /** Returns the IANA timezone string, e.g. "Asia/Taipei" */
  public static String getTimezone() {
    return TIMEZONE.get();
  }

  /** Returns the tenant's ZoneId, defaults to UTC if not set */
  public static ZoneId getZoneId() {
    String tz = TIMEZONE.get();
    return tz != null ? ZoneId.of(tz) : ZoneOffset.UTC;
  }

  public static void clear() {
    TENANT_ID.remove();
    TIMEZONE.remove();
  }
}
