package net.lab1024.sa.common.mq.kafka.event;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.listener.AbstractKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;

/**
 * Base event listener with tenant context propagation
 *
 * <p>Automatically extracts tenantId from DomainEvent and sets TenantContext. Ensures multi-tenant
 * data isolation during event processing.
 *
 * @param <T> message type
 * @author iGaming Team
 * @since 2026-02-14
 */
@Slf4j
public abstract class DomainEventListener<T> extends AbstractKafkaListener<T> {

  @Override
  protected void doHandle(ConsumerRecord<String, String> record) {
    DomainEvent event = JsonUtil.fromJson(record.value(), DomainEvent.class);

    // Propagate tenant context
    Long tenantId = event.getTenantId();
    if (tenantId != null) {
      TenantContext.setTenantId(tenantId);
    }

    // Propagate trace context
    String traceId = event.getTraceId();
    if (traceId != null) {
      MDC.put("traceId", traceId);
    }

    try {
      log.debug(
          "Processing event: eventType={}, eventId={}, tenantId={}",
          event.getEventType(),
          event.getEventId(),
          tenantId);

      onEvent(event);
    } finally {
      // Clean up context to prevent leakage between messages
      TenantContext.clear();
      MDC.remove("traceId");
    }
  }

  /**
   * Handle the domain event (subclass implementation)
   *
   * @param event the domain event to handle
   */
  protected abstract void onEvent(DomainEvent event);
}
