package net.lab1024.sa.common.tenant.service;

import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.tenant.dao.TenantDao;
import net.lab1024.sa.common.tenant.domain.TenantEntity;
import net.lab1024.sa.common.tenant.domain.TenantInfo;
import org.springframework.stereotype.Service;

/**
 * Tenant service — cached lookup by tenant code.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

  private final TenantDao tenantDao;

  private final ConcurrentHashMap<String, TenantInfo> tenantCache = new ConcurrentHashMap<>();

  /**
   * Look up tenant info by code (subdomain prefix). Results are cached in-memory.
   *
   * @param tenantCode the subdomain prefix, e.g. "tenant1"
   * @return tenant info, or null if not found or disabled
   */
  public TenantInfo getTenantByCode(String tenantCode) {
    return tenantCache.computeIfAbsent(
        tenantCode,
        code -> {
          TenantEntity entity = tenantDao.getByCode(code);
          if (entity == null || !Boolean.TRUE.equals(entity.getEnabledFlag())) {
            return null;
          }
          return new TenantInfo(
              entity.getTenantId(),
              entity.getTenantCode(),
              entity.getTenantName(),
              entity.getTimezone());
        });
  }

  /** Clear the tenant cache. Integrates with @SmartReload pattern. */
  public void clearCache() {
    tenantCache.clear();
    log.info("Tenant cache cleared");
  }
}
