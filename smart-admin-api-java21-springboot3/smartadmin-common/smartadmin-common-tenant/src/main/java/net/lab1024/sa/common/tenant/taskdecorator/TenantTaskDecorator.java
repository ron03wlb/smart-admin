package net.lab1024.sa.common.tenant.taskdecorator;

import net.lab1024.sa.common.core.tenant.TenantContext;
import org.springframework.core.task.TaskDecorator;

/**
 * Task decorator that propagates TenantContext to child threads.
 *
 * <p>Required for @Async methods and Virtual Thread execution to maintain tenant isolation.
 * Captures TenantContext from the calling thread and restores it in the executing thread.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public class TenantTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    Long tenantId = TenantContext.getTenantId();
    String timezone = TenantContext.getTimezone();

    return () -> {
      try {
        if (tenantId != null) {
          TenantContext.setTenantId(tenantId);
        }
        if (timezone != null) {
          TenantContext.setTimezone(timezone);
        }
        runnable.run();
      } finally {
        TenantContext.clear();
      }
    };
  }
}
