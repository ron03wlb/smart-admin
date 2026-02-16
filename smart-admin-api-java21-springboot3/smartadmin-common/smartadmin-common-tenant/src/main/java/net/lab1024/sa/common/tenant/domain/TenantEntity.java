package net.lab1024.sa.common.tenant.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Tenant entity — the tenant registry table.
 *
 * <p>Does NOT extend SmartAdminBaseEntity because it has no tenant_id column itself.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Data
@TableName("t_tenant")
public class TenantEntity {

  @TableId(type = IdType.AUTO)
  private Long tenantId;

  /** Subdomain prefix, e.g. "tenant1" */
  private String tenantCode;

  /** Display name */
  private String tenantName;

  /** IANA timezone, e.g. "Asia/Taipei" */
  private String timezone;

  /** Whether this tenant is active */
  private Boolean enabledFlag;

  private OffsetDateTime createTime;

  private OffsetDateTime updateTime;
}
