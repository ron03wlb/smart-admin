# Message Queue Pattern Generator - Examples

## 範例 1: 訂單事件發布/訂閱

**事件類**:
```java
public sealed interface OrderEvent permits
    OrderCreatedEvent, OrderPaidEvent, OrderShippedEvent, OrderCompletedEvent {

    String getEventId();
    Long getOrderId();
    LocalDateTime getOccurredAt();
}

@Data @Builder
public final class OrderCreatedEvent implements OrderEvent {
    private String eventId;
    private Long orderId;
    private Long customerId;
    private BigDecimal totalAmount;
    private LocalDateTime occurredAt;
}
```

**發布者**:
```java
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send("order-events", event.getOrderId().toString(), event)
            .addCallback(
                result -> log.info("Event sent: {}", event.getEventId()),
                ex -> log.error("Failed to send event", ex)
            );
    }
}
```

**訂閱者**:
```java
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderEvent(@Payload OrderEvent event, Acknowledgment ack) {
        switch (event) {
            case OrderCreatedEvent created -> reserveInventory(created);
            case OrderPaidEvent paid -> confirmInventory(paid);
            case OrderCompletedEvent completed -> {} // no-op
            default -> log.warn("Unknown event type: {}", event.getClass());
        }
        ack.acknowledge();
    }

    private void reserveInventory(OrderCreatedEvent event) {
        // 預留庫存邏輯
    }
}
```

---

## 範例 2: Saga 分散式事務

```java
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    private final KafkaTemplate<String, SagaCommand> kafkaTemplate;

    public void startOrderSaga(CreateOrderCommand cmd) {
        // Step 1: 創建訂單
        kafkaTemplate.send("order-commands", new CreateOrderSagaStep(cmd));
    }

    @KafkaListener(topics = "saga-order-created")
    public void onOrderCreated(OrderCreatedReply reply) {
        // Step 2: 扣減庫存
        kafkaTemplate.send("inventory-commands", new DeductInventoryStep(reply.getOrderId()));
    }

    @KafkaListener(topics = "saga-inventory-deducted")
    public void onInventoryDeducted(InventoryDeductedReply reply) {
        // Step 3: 扣款
        kafkaTemplate.send("payment-commands", new ProcessPaymentStep(reply.getOrderId()));
    }

    @KafkaListener(topics = "saga-payment-failed")
    public void onPaymentFailed(PaymentFailedReply reply) {
        // 補償: 恢復庫存
        kafkaTemplate.send("inventory-commands", new RestoreInventoryStep(reply.getOrderId()));
        // 補償: 取消訂單
        kafkaTemplate.send("order-commands", new CancelOrderStep(reply.getOrderId()));
    }
}
```

---

## 範例 3: RocketMQ 整合

```java
@Service
@RequiredArgsConstructor
public class RocketMQOrderPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        rocketMQTemplate.asyncSend("order-topic:created", event, new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                log.info("Message sent: {}", result.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("Failed to send message", e);
            }
        });
    }
}

@Component
@RocketMQMessageListener(
    topic = "order-topic",
    selectorExpression = "created || paid",
    consumerGroup = "notification-consumer"
)
public class RocketMQOrderConsumer implements RocketMQListener<OrderEvent> {

    @Override
    public void onMessage(OrderEvent event) {
        // 處理消息
    }
}
```
