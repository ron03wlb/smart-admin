package net.lab1024.sa.common.mq.kafka.event;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.slf4j.MDC;
import org.springframework.kafka.support.SendResult;

/**
 * Unified domain event publisher
 *
 * <p>Auto-populates tenantId from TenantContext and traceId from MDC. Uses KafkaProducerService for
 * reliable message delivery.
 *
 * <p>Bean is registered by {@link net.lab1024.sa.common.mq.kafka.config.EventAutoConfiguration}
 * when smart.kafka.enabled=true.
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Slf4j
@RequiredArgsConstructor
public class DomainEventPublisher {

  private static final String TRACE_ID_KEY = "traceId";

  private final KafkaProducerService kafkaProducerService;

  /**
   * Publish a domain event asynchronously
   *
   * @param topic Kafka topic name
   * @param event domain event to publish
   * @return CompletableFuture of send result
   */
  public CompletableFuture<SendResult<String, String>> publish(String topic, DomainEvent event) {

    // Auto-populate traceId from MDC
    if (event.getTraceId() == null) {
      event.setTraceId(MDC.get(TRACE_ID_KEY));
    }

    // Auto-populate tenantId from TenantContext
    if (event.getTenantId() == null) {
      event.setTenantId(TenantContext.getTenantId());
    }

    String message = JsonUtil.toJson(event);
    String key = event.getAggregateId();

    log.info(
        "Publishing domain event: topic={}, eventType={}, eventId={}, aggregateId={}",
        topic,
        event.getEventType(),
        event.getEventId(),
        key);

    return kafkaProducerService
        .sendAsync(topic, key, message)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error(
                    "Failed to publish domain event: eventId={}, eventType={}, error={}",
                    event.getEventId(),
                    event.getEventType(),
                    ex.getMessage(),
                    ex);
              } else {
                log.debug(
                    "Domain event published: eventId={}, partition={}, offset={}",
                    event.getEventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
              }
            });
  }

  /**
   * Publish a domain event synchronously (for critical financial operations)
   *
   * @param topic Kafka topic name
   * @param event domain event to publish
   * @return true if published successfully
   */
  public boolean publishSync(String topic, DomainEvent event) {
    if (event.getTraceId() == null) {
      event.setTraceId(MDC.get(TRACE_ID_KEY));
    }
    if (event.getTenantId() == null) {
      event.setTenantId(TenantContext.getTenantId());
    }

    String message = JsonUtil.toJson(event);
    String key = event.getAggregateId();

    return kafkaProducerService.sendSync(topic, key, message).isDefined();
  }
}
