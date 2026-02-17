package net.lab1024.sa.app.tenant;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.mybatis.handler.SmartTenantLineHandler;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for multi-tenant infrastructure components.
 *
 * <p>Tests TenantContext (ThreadLocal), SmartTenantLineHandler (SQL injection), and their
 * interaction. Does NOT require Spring context or Testcontainers.
 *
 * @since 2026-02-17
 */
class TenantInfrastructureTest {

  @AfterEach
  void cleanup() {
    TenantContext.clear();
  }

  // ==================== TenantContext ====================

  @Test
  void tenantContextSetAndGetTenantId() {
    assertNull(TenantContext.getTenantId());

    TenantContext.setTenantId(42L);
    assertEquals(42L, TenantContext.getTenantId());
  }

  @Test
  void tenantContextSetAndGetTimezone() {
    assertNull(TenantContext.getTimezone());

    TenantContext.setTimezone("Asia/Taipei");
    assertEquals("Asia/Taipei", TenantContext.getTimezone());
  }

  @Test
  void tenantContextGetZoneIdDefaultsToUtc() {
    assertEquals(ZoneOffset.UTC, TenantContext.getZoneId());
  }

  @Test
  void tenantContextGetZoneIdReturnsConfiguredTimezone() {
    TenantContext.setTimezone("America/New_York");
    assertEquals(ZoneId.of("America/New_York"), TenantContext.getZoneId());
  }

  @Test
  void tenantContextClearRemovesBothValues() {
    TenantContext.setTenantId(1L);
    TenantContext.setTimezone("UTC");
    TenantContext.clear();

    assertNull(TenantContext.getTenantId());
    assertNull(TenantContext.getTimezone());
  }

  @Test
  void tenantContextIsThreadIsolated() throws Exception {
    TenantContext.setTenantId(100L);

    Thread other =
        new Thread(
            () -> {
              assertNull(TenantContext.getTenantId());
              TenantContext.setTenantId(200L);
              assertEquals(200L, TenantContext.getTenantId());
              TenantContext.clear();
            });
    other.start();
    other.join();

    assertEquals(100L, TenantContext.getTenantId());
  }

  // ==================== SmartTenantLineHandler ====================

  @Test
  void handlerReturnsTenantIdFromContext() {
    TenantContext.setTenantId(42L);
    SmartTenantLineHandler handler = new SmartTenantLineHandler();

    assertInstanceOf(LongValue.class, handler.getTenantId());
    assertEquals(42L, ((LongValue) handler.getTenantId()).getValue());
  }

  @Test
  void handlerReturnsNullValueWhenNoContext() {
    SmartTenantLineHandler handler = new SmartTenantLineHandler();

    assertInstanceOf(NullValue.class, handler.getTenantId());
  }

  @Test
  void handlerUsesCorrectColumnName() {
    SmartTenantLineHandler handler = new SmartTenantLineHandler();
    assertEquals("tenant_id", handler.getTenantIdColumn());
  }

  @Test
  void handlerIgnoresTenantTable() {
    SmartTenantLineHandler handler = new SmartTenantLineHandler();

    assertTrue(handler.ignoreTable("t_tenant"));
    assertTrue(handler.ignoreTable("flyway_schema_history"));
    assertFalse(handler.ignoreTable("t_employee"));
    assertFalse(handler.ignoreTable("t_player"));
  }
}
