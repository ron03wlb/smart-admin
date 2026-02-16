package net.lab1024.sa.common.tenant.interceptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * MyBatis Interceptor that sets PostgreSQL session variable for Row-Level Security.
 *
 * <p>Before each SQL execution, sets {@code app.current_tenant_id} via {@code SET LOCAL} so that
 * PostgreSQL RLS policies can filter rows by tenant. Uses {@code set_config(name, value, true)}
 * where {@code is_local=true} ensures the value is transaction-scoped and auto-resets on
 * commit/rollback — no connection pool leakage.
 *
 * <p>Only active when {@code tenant.rls.enabled=true} (bean registration gated by
 * TenantAutoConfiguration).
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Slf4j
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class})
})
public class RlsSessionInterceptor implements Interceptor {

  private static final String SET_TENANT_SQL =
      "SELECT set_config('app.current_tenant_id', ?, true)";

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId != null) {
      setTenantSessionVariable(invocation, tenantId);
    }
    return invocation.proceed();
  }

  private void setTenantSessionVariable(Invocation invocation, Long tenantId) throws SQLException {
    Executor executor = (Executor) invocation.getTarget();
    Transaction transaction = executor.getTransaction();
    Connection connection = transaction.getConnection();
    try (PreparedStatement ps = connection.prepareStatement(SET_TENANT_SQL)) {
      ps.setString(1, tenantId.toString());
      ps.execute();
    }
  }
}
