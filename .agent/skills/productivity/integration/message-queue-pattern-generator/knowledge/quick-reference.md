# Message Queue Pattern Generator - Quick Reference

## Kafka 配置

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: smartadmin-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

## 事件類設計

```java
@Data
@Builder
public class OrderStatusChangedEvent {
    private String eventId;        // 唯一事件 ID (冪等性)
    private Long orderId;
    private String previousStatus;
    private String currentStatus;
    private LocalDateTime changedAt;
    private String changedBy;
}
```

## Producer 發送

```java
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderStatusChangedEvent> kafkaTemplate;

    public void publishStatusChanged(Long orderId, String prevStatus, String currStatus) {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .orderId(orderId)
            .previousStatus(prevStatus)
            .currentStatus(currStatus)
            .changedAt(LocalDateTime.now())
            .build();

        kafkaTemplate.send("order-status-changed", orderId.toString(), event);
    }
}
```

## Consumer 監聽

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "order-status-changed", groupId = "notification-group")
    public void handleOrderStatusChanged(
            @Payload OrderStatusChangedEvent event,
            Acknowledgment ack) {

        // 冪等性檢查
        String key = "event:processed:" + event.getEventId();
        if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24)))) {
            log.info("Event already processed: {}", event.getEventId());
            ack.acknowledge();
            return;
        }

        try {
            processEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process event", e);
            // 不 ack，將重試
        }
    }
}
```

## 死信隊列

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate),
        new FixedBackOff(1000L, 3)  // 重試 3 次，間隔 1 秒
    ));
    return factory;
}
```
