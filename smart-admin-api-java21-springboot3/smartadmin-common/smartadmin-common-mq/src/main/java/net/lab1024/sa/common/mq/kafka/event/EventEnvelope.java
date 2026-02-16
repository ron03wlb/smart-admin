package net.lab1024.sa.common.mq.kafka.event;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka message envelope wrapping headers and payload
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope {

  /** Kafka headers (eventType, tenantId, traceId, version) */
  private Map<String, String> headers;

  /** Serialized DomainEvent JSON payload */
  private String payload;

  /** Source topic */
  private String topic;

  /** Partition number */
  private Integer partition;

  /** Message offset */
  private Long offset;

  /** Message key (aggregateId) */
  private String key;
}
