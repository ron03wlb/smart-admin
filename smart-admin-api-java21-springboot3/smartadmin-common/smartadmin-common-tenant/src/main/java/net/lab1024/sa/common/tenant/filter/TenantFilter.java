package net.lab1024.sa.common.tenant.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.tenant.TenantProperties;
import net.lab1024.sa.common.tenant.domain.TenantInfo;
import net.lab1024.sa.common.tenant.service.TenantService;
import org.slf4j.MDC;

/**
 * Servlet filter that resolves tenant from subdomain.
 *
 * <p>Extracts tenant code from the Host header subdomain, resolves it to a tenant ID and timezone,
 * and stores them in TenantContext for downstream consumption.
 *
 * <p>Order: runs before Sa-Token auth filter.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Slf4j
@RequiredArgsConstructor
public class TenantFilter implements Filter {

  private final TenantService tenantService;

  private final TenantProperties tenantProperties;

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    try {
      String host = request.getServerName();
      String tenantCode = extractTenantCode(host);

      if (tenantCode == null || tenantCode.isEmpty()) {
        log.warn("Cannot extract tenant code from host: {}", host);
        ((HttpServletResponse) res).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
        return;
      }

      TenantInfo tenantInfo = tenantService.getTenantByCode(tenantCode);
      if (tenantInfo == null) {
        log.warn("Tenant not found or disabled: {}", tenantCode);
        ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found");
        return;
      }

      TenantContext.setTenantId(tenantInfo.getTenantId());
      TenantContext.setTimezone(tenantInfo.getTimezone());

      MDC.put("tenantId", String.valueOf(tenantInfo.getTenantId()));
      MDC.put("tenantCode", tenantCode);

      chain.doFilter(req, res);
    } finally {
      TenantContext.clear();
      MDC.remove("tenantId");
      MDC.remove("tenantCode");
    }
  }

  /**
   * Extract tenant code from host header.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>"tenant1.example.com" → "tenant1"
   *   <li>"tenant1.localhost" → "tenant1"
   *   <li>"localhost" → "default"
   *   <li>"example.com" → "default"
   * </ul>
   */
  private String extractTenantCode(String host) {
    if (host == null) {
      return null;
    }
    // Remove port if present
    String hostWithoutPort = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;

    String baseDomain = tenantProperties.getBaseDomain();

    // Check if host is "subdomain.baseDomain"
    if (hostWithoutPort.endsWith("." + baseDomain)) {
      String prefix =
          hostWithoutPort.substring(0, hostWithoutPort.length() - baseDomain.length() - 1);
      // Only take the first subdomain level
      int dotIndex = prefix.lastIndexOf('.');
      return dotIndex >= 0 ? prefix.substring(dotIndex + 1) : prefix;
    }

    // Handle localhost: "tenant1.localhost" → "tenant1"
    if (hostWithoutPort.endsWith(".localhost")) {
      return hostWithoutPort.substring(0, hostWithoutPort.length() - ".localhost".length());
    }

    // No subdomain — bare domain or localhost
    if (hostWithoutPort.equals("localhost") || hostWithoutPort.equals(baseDomain)) {
      return "default";
    }

    return "default";
  }
}
