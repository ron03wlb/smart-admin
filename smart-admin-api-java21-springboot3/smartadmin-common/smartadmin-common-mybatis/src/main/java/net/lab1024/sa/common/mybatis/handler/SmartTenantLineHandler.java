package net.lab1024.sa.common.mybatis.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import java.util.Set;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

/**
 * MyBatis-Plus TenantLineHandler implementation.
 *
 * <p>Reads tenant ID from TenantContext and injects it into all SQL queries via the
 * TenantLineInnerInterceptor.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public class SmartTenantLineHandler implements TenantLineHandler {

  /** Tables that should NOT have tenant filtering applied */
  private static final Set<String> IGNORE_TABLES =
      Set.of(
          "t_tenant", // Tenant registry itself
          "flyway_schema_history" // Flyway internal
          );

  @Override
  public Expression getTenantId() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      // Return NullValue to avoid NPE; interceptor will handle appropriately
      return new NullValue();
    }
    return new LongValue(tenantId);
  }

  @Override
  public String getTenantIdColumn() {
    return "tenant_id";
  }

  @Override
  public boolean ignoreTable(String tableName) {
    return IGNORE_TABLES.contains(tableName);
  }
}
