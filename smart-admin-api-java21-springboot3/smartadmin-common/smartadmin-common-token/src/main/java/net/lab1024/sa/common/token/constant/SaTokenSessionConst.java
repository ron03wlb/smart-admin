package net.lab1024.sa.common.token.constant;

/**
 * Constants for Sa-Token session attribute keys.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public final class SaTokenSessionConst {

  private SaTokenSessionConst() {}

  /** Tenant ID stored in session after login */
  public static final String TENANT_ID = "tenantId";

  /** Timezone stored in session after login */
  public static final String TIMEZONE = "timezone";
}
