package net.lab1024.sa.common.tenant.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Tenant info BO — lightweight tenant data cached in memory.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Data
@AllArgsConstructor
public class TenantInfo {

  private Long tenantId;

  private String tenantCode;

  private String tenantName;

  /** IANA timezone, e.g. "Asia/Taipei" */
  private String timezone;
}
