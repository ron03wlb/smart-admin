package net.lab1024.sa.igaming.game.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Callback Tenant Interceptor — overrides TenantContext for external GP/PSP callbacks.
 *
 * <p>TenantFilter resolves tenant from subdomain, but GP callbacks may not use subdomain routing.
 * This interceptor reads the {@code X-Tenant-Id} header and overrides TenantContext so that
 * TenantLineInnerInterceptor injects the correct tenant_id into SQL queries.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Slf4j
public class CallbackTenantInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String headerValue = request.getHeader("X-Tenant-Id");
    if (headerValue != null && !headerValue.isEmpty()) {
      try {
        Long tenantId = Long.valueOf(headerValue);
        TenantContext.setTenantId(tenantId);
        log.debug("Callback tenant override: tenantId={}", tenantId);
      } catch (NumberFormatException e) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return false;
      }
    }
    return true;
  }
}
