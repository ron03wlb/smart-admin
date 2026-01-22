# Kafka Batch Quickstart Guide

本文档提供 SmartAdmin Kafka 批量功能的快速入门指南。

## 目录

- [环境要求](#环境要求)
- [快速配置](#快速配置)
- [Producer 批量发送](#producer-批量发送)
- [Consumer 批量消费](#consumer-批量消费)
- [MessageAggregator 消息聚合](#messageaggregator-消息聚合)
- [常见问题](#常见问题)

## 环境要求

- Java 21+
- Spring Boot 3.x
- Apache Kafka 3.x
- SmartAdmin Framework

## 快速配置

### 1. 添加依赖

在 `build.gradle.kts` 中添加 mq 模块依赖：

```kotlin
dependencies {
    implementation(project(":sa-base:foundation:mq"))
}
```

### 2. 配置 application.yml

```yaml
smart:
  kafka:
    enabled: true  # 启用 Kafka
    bootstrap-servers: localhost:9092

    # 批量功能配置
    batch:
      enabled: true      # 启用批量消费模式
      size: 100          # 批量消费大小
      concurrency: 3     # 并发消费者数量

      # 消息聚合配置（可选）
      aggregate:
        enabled: true    # 启用消息聚合
        count: 50        # 聚合数量阈值
        timeout-ms: 5000 # 聚合超时时间（毫秒）

    # 死信队列配置
    dead-letter-queue:
      enabled: true
      topic-suffix: .dlq
      retry:
        max-attempts: 3
        initial-interval: 1000
        multiplier: 2.0
```

## Producer 批量发送

### 异步批量发送（推荐）

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaProducerService kafkaProducerService;

    public void sendOrders(List<Order> orders) {
        // 转换为消息列表
        List<String> messages = orders.stream()
            .map(this::toJson)
            .toList();

        // 异步批量发送
        kafkaProducerService.sendBatchAsync("order-topic", messages)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("批量发送异常: {}", ex.getMessage(), ex);
                    return;
                }

                if (result.isAllSuccess()) {
                    log.info("全部发送成功: total={}", result.getTotal());
                } else if (result.isPartialSuccess()) {
                    log.warn("部分发送成功: success={}, failed={}",
                        result.getSuccessCount(), result.getFailureCount());
                    // 处理失败消息
                    handleFailedMessages(result.getFailedMessages());
                }
            });
    }
}
```

### 带 Key 的批量发送（保证顺序）

```java
public void sendOrdersWithKey(List<Order> orders) {
    // 使用订单ID作为 key，保证同一订单的消息顺序
    List<Map.Entry<String, String>> keyedMessages = orders.stream()
        .map(order -> new AbstractMap.SimpleEntry<>(
            order.getOrderId(),  // key = orderId
            toJson(order)        // value = JSON
        ))
        .toList();

    kafkaProducerService.sendBatchAsyncWithKeys("order-topic", keyedMessages)
        .whenComplete((result, ex) -> {
            // 处理结果...
        });
}
```

### 同步批量发送（需要确认结果）

```java
public BatchSendResult sendOrdersSync(List<Order> orders) {
    List<String> messages = orders.stream()
        .map(this::toJson)
        .toList();

    // 同步发送，阻塞等待结果
    BatchSendResult result = kafkaProducerService.sendBatchSync(
        "order-topic",
        messages,
        30000  // 超时时间（毫秒）
    );

    if (!result.isAllSuccess()) {
        // 记录或处理失败消息
        for (BatchSendResult.FailedMessage failed : result.getFailedMessages()) {
            log.error("消息发送失败: index={}, error={}",
                failed.getIndex(), failed.getErrorMessage());
        }
    }

    return result;
}
```

## Consumer 批量消费

### 创建批量消费者

继承 `AbstractBatchKafkaListener` 实现批量消费：

```java
@Component
@Slf4j
@ConditionalOnProperty(prefix = "smart.kafka.batch", name = "enabled", havingValue = "true")
public class OrderBatchListener extends AbstractBatchKafkaListener<Order> {

    private final OrderService orderService;

    public OrderBatchListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 监听消息
     * 使用 batchKafkaListenerContainerFactory 启用批量模式
     */
    @KafkaListener(
        topics = "order-topic",
        groupId = "order-consumer-group",
        containerFactory = "batchKafkaListenerContainerFactory")
    public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        handleBatch(records, ack);  // 调用父类模板方法
    }

    /**
     * 批量处理逻辑
     * 处理失败时会自动降级为逐条处理
     */
    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception {
        List<Order> orders = records.stream()
            .map(r -> parseJson(r.value(), Order.class))
            .toList();

        // 批量插入数据库
        orderService.batchInsert(orders);

        log.info("批量处理完成: count={}", orders.size());
    }

    /**
     * 单条处理逻辑（降级时使用）
     * 处理失败的消息会发送到 DLQ
     */
    @Override
    protected void doHandle(ConsumerRecord<String, String> record) throws Exception {
        Order order = parseJson(record.value(), Order.class);
        orderService.insert(order);
        log.debug("单条处理完成: orderId={}", order.getOrderId());
    }
}
```

### 处理流程说明

```
接收批量消息
    │
    ▼
尝试批量处理 (doBatchHandle)
    │
    ├── 成功 ─────► 统一确认消息
    │
    └── 失败 ─────► 降级为逐条处理 (doHandle)
                        │
                        ├── 成功 ─────► 继续处理下一条
                        │
                        └── 失败 ─────► 发送到 DLQ
                                            │
                                            ▼
                                    统一确认所有消息
```

## MessageAggregator 消息聚合

适用于高频小消息场景，如日志、埋点、监控数据。

### 基本使用

```java
@Component
@Slf4j
@ConditionalOnProperty(prefix = "smart.kafka.batch.aggregate", name = "enabled", havingValue = "true")
public class LogAggregatorService {

    private final KafkaProducerService kafkaProducerService;
    private MessageAggregator<String> aggregator;

    public LogAggregatorService(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostConstruct
    public void init() {
        aggregator = new MessageAggregator<>(
            50,      // 累积 50 条消息后触发
            5000,    // 或超时 5 秒后触发
            messages -> {
                // 批量发送回调
                kafkaProducerService.sendBatchAsync("log-topic", messages)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("日志发送失败: {}", ex.getMessage());
                        }
                    });
            }
        );
    }

    @PreDestroy
    public void destroy() {
        if (aggregator != null) {
            aggregator.shutdown();  // 关闭时刷新剩余消息
        }
    }

    /**
     * 添加日志消息
     * 达到阈值或超时后自动批量发送
     */
    public void addLog(String logMessage) {
        aggregator.add(logMessage);
    }

    /**
     * 强制刷新（立即发送）
     */
    public void flush() {
        aggregator.flush();
    }

    /**
     * 获取当前缓冲区大小
     */
    public int getBufferSize() {
        return aggregator.getBufferSize();
    }
}
```

### 工作原理

```
添加消息 ─────► 缓冲区
                 │
                 ├── 达到数量阈值 ─────► 触发批量处理
                 │
                 └── 超时 ─────────────► 触发批量处理

手动 flush() ───────────────────────────► 立即处理
shutdown() ─────────────────────────────► 刷新剩余消息并关闭
```

## 常见问题

### Q1: 如何选择批量大小？

**建议**：根据消息大小和处理时间调整
- 小消息（< 1KB）：100-500 条/批
- 中等消息（1-10KB）：50-100 条/批
- 大消息（> 10KB）：10-50 条/批

### Q2: 批量处理失败后如何处理？

**流程**：
1. 自动降级为逐条处理
2. 失败的消息发送到 DLQ（死信队列）
3. 从 DLQ 读取消息进行人工处理或重试

### Q3: 如何保证消息顺序？

**方案**：使用带 key 的发送方法
```java
// 相同 key 的消息会路由到同一分区
kafkaProducerService.sendBatchAsyncWithKeys(topic, keyedMessages);
```

### Q4: MessageAggregator 是否线程安全？

**是的**，MessageAggregator 使用 `ReentrantLock` 保证线程安全，可以在多线程环境下使用。

### Q5: 如何监控批量处理的性能？

**方法**：
1. 检查 `BatchSendResult` 中的成功/失败数量
2. 配置 Kafka 监控指标（Micrometer）
3. 添加自定义日志记录处理时间

### Q6: DLQ 消息如何处理？

**建议流程**：
1. 创建 DLQ 消费者监听死信队列
2. 记录失败消息到数据库
3. 提供管理界面查看和重试
4. 设置告警通知

```java
@KafkaListener(topics = "order-topic.dlq", groupId = "dlq-handler")
public void handleDlq(ConsumerRecord<String, String> record) {
    // 记录到数据库
    dlqService.save(record);
    // 发送告警
    alertService.sendAlert("消息处理失败", record.value());
}
```

## 下一步

- [API 参考文档](kafka-batch-api-reference.md) - 详细的 API 说明
- [示例代码](kafka-batch-examples.md) - 更多业务场景示例
- [测试指南](kafka-batch-testing-guide.md) - 如何测试 Kafka 批量功能
