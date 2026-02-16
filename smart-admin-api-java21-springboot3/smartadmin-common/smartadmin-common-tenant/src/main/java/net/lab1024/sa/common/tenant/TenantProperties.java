package net.lab1024.sa.common.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tenant configuration properties.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Data
@ConfigurationProperties(prefix = "tenant")
public class TenantProperties {

  /** Whether multi-tenant is enabled */
  private boolean enabled = false;

  /** Default timezone for tenants without explicit timezone */
  private String defaultTimezone = "UTC";

  /** Base domain for subdomain extraction (e.g. "example.com") */
  private String baseDomain = "example.com";

  /** PostgreSQL Row-Level Security configuration */
  private Rls rls = new Rls();

  /** RLS sub-configuration */
  @Data
  public static class Rls {

    /** Whether PostgreSQL RLS session variable injection is enabled */
    private boolean enabled = false;
  }
}
