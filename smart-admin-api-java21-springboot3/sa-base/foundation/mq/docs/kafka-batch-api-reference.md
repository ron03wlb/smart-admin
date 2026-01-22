# Kafka Batch API Reference

本文档提供 SmartAdmin Kafka 批量功能的完整 API 参考。

## 目录

- [KafkaProducerService](#kafkaproducerservice)
- [AbstractBatchKafkaListener](#abstractbatchkafkalistener)
- [MessageAggregator](#messageaggregator)
- [BatchSendResult](#batchsendresult)
- [配置属性](#配置属性)

---

## KafkaProducerService

Kafka 生产者服务接口，提供单条和批量消息发送功能。

### 接口定义

```java
public interface KafkaProducerService {
    // 单条发送
    CompletableFuture<SendResult<String, String>> sendAsync(String topic, String message);
    CompletableFuture<SendResult<String, String>> sendAsync(String topic, String key, String message);
    Optional<SendResult<String, String>> sendSync(String topic, String message);
    Optional<SendResult<String, String>> sendSync(String topic, String key, String message);
    Optional<SendResult<String, String>> sendSync(String topic, String key, String message, long timeoutMs);

    // 批量发送
    CompletableFuture<BatchSendResult> sendBatchAsync(String topic, List<String> messages);
    CompletableFuture<BatchSendResult> sendBatchAsyncWithKeys(String topic, List<Map.Entry<String, String>> keyedMessages);
    BatchSendResult sendBatchSync(String topic, List<String> messages, long timeoutMs);
    BatchSendResult sendBatchSyncWithKeys(String topic, List<Map.Entry<String, String>> keyedMessages, long timeoutMs);
}
```

### 方法详细说明

#### sendAsync(String topic, String message)

异步发送消息（无 key）。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| message | String | 消息内容 |
| **返回值** | CompletableFuture<SendResult> | 发送结果的 Future |

**示例**：
```java
kafkaProducerService.sendAsync("my-topic", "Hello World")
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("发送失败", ex);
        } else {
            log.info("发送成功, offset={}", result.getRecordMetadata().offset());
        }
    });
```

#### sendAsync(String topic, String key, String message)

异步发送消息（带 key），相同 key 的消息会路由到同一分区。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| key | String | 消息 Key（用于分区路由） |
| message | String | 消息内容 |
| **返回值** | CompletableFuture<SendResult> | 发送结果的 Future |

#### sendSync(String topic, String message)

同步发送消息（无 key），使用默认超时时间（10秒）。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| message | String | 消息内容 |
| **返回值** | Optional<SendResult> | 发送结果，失败返回 empty |

#### sendSync(String topic, String key, String message, long timeoutMs)

同步发送消息（带 key，自定义超时）。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| key | String | 消息 Key |
| message | String | 消息内容 |
| timeoutMs | long | 超时时间（毫秒） |
| **返回值** | Optional<SendResult> | 发送结果，失败/超时返回 empty |

#### sendBatchAsync(String topic, List<String> messages)

异步批量发送消息（无 key）。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| messages | List<String> | 消息列表 |
| **返回值** | CompletableFuture<BatchSendResult> | 批量发送结果的 Future |

**示例**：
```java
List<String> messages = List.of("msg1", "msg2", "msg3");
kafkaProducerService.sendBatchAsync("my-topic", messages)
    .whenComplete((result, ex) -> {
        if (result.isAllSuccess()) {
            log.info("全部发送成功");
        } else {
            log.warn("部分失败: {}/{}", result.getFailureCount(), result.getTotal());
        }
    });
```

#### sendBatchAsyncWithKeys(String topic, List<Map.Entry<String, String>> keyedMessages)

异步批量发送消息（带 key），保证相同 key 的消息顺序。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| keyedMessages | List<Map.Entry<String, String>> | Key-Value 消息列表 |
| **返回值** | CompletableFuture<BatchSendResult> | 批量发送结果的 Future |

**示例**：
```java
List<Map.Entry<String, String>> keyedMessages = List.of(
    new AbstractMap.SimpleEntry<>("user-1", "action-1"),
    new AbstractMap.SimpleEntry<>("user-1", "action-2"),  // 同一 key，保证顺序
    new AbstractMap.SimpleEntry<>("user-2", "action-3")
);
kafkaProducerService.sendBatchAsyncWithKeys("user-actions", keyedMessages);
```

#### sendBatchSync(String topic, List<String> messages, long timeoutMs)

同步批量发送消息（无 key）。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| messages | List<String> | 消息列表 |
| timeoutMs | long | 超时时间（毫秒） |
| **返回值** | BatchSendResult | 批量发送结果 |

#### sendBatchSyncWithKeys(String topic, List<Map.Entry<String, String>> keyedMessages, long timeoutMs)

同步批量发送消息（带 key）。

| 参数 | 类型 | 说明 |
|------|------|------|
| topic | String | Topic 名称 |
| keyedMessages | List<Map.Entry<String, String>> | Key-Value 消息列表 |
| timeoutMs | long | 超时时间（毫秒） |
| **返回值** | BatchSendResult | 批量发送结果 |

---

## AbstractBatchKafkaListener

批量消费者抽象基类，提供批量处理、降级单条处理、DLQ 发送等模板方法。

### 类定义

```java
public abstract class AbstractBatchKafkaListener<T> {

    protected void handleBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack);

    protected abstract void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception;

    protected abstract void doHandle(ConsumerRecord<String, String> record) throws Exception;

    protected String getListenerName();
}
```

### 方法详细说明

#### handleBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack)

处理批量消息的模板方法，在 `@KafkaListener` 方法中调用。

| 参数 | 类型 | 说明 |
|------|------|------|
| records | List<ConsumerRecord> | 消息记录列表 |
| ack | Acknowledgment | 确认对象 |

**处理流程**：
1. 尝试批量处理 `doBatchHandle()`
2. 批量处理失败时降级为逐条处理 `doHandle()`
3. 逐条处理失败的消息发送到 DLQ
4. 统一确认所有消息

#### doBatchHandle(List<ConsumerRecord<String, String>> records)

**抽象方法** - 批量处理逻辑，子类必须实现。

| 参数 | 类型 | 说明 |
|------|------|------|
| records | List<ConsumerRecord> | 消息记录列表 |

**注意**：
- 抛出异常会触发降级为逐条处理
- 应实现高效的批量操作（如批量插入数据库）

#### doHandle(ConsumerRecord<String, String> record)

**抽象方法** - 单条处理逻辑，子类必须实现。

| 参数 | 类型 | 说明 |
|------|------|------|
| record | ConsumerRecord | 单条消息记录 |

**注意**：
- 批量处理失败时会逐条调用此方法
- 抛出异常的消息会发送到 DLQ

### 使用示例

```java
@Component
public class OrderBatchListener extends AbstractBatchKafkaListener<Order> {

    @KafkaListener(
        topics = "order-topic",
        groupId = "order-group",
        containerFactory = "batchKafkaListenerContainerFactory")
    public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        handleBatch(records, ack);
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception {
        // 批量处理
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) throws Exception {
        // 单条处理
    }
}
```

---

## MessageAggregator

消息聚合器，将消息累积到一定数量或超时后触发批量处理。

### 类定义

```java
public class MessageAggregator<T> {

    public MessageAggregator(int countThreshold, long timeoutMs, Consumer<List<T>> batchHandler);

    public void add(T message);
    public void addAll(List<T> messages);
    public void flush();
    public void shutdown();
    public int getBufferSize();
}
```

### 构造函数

```java
public MessageAggregator(int countThreshold, long timeoutMs, Consumer<List<T>> batchHandler)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| countThreshold | int | 数量阈值（必须 > 0） |
| timeoutMs | long | 超时时间（毫秒，必须 > 0） |
| batchHandler | Consumer<List<T>> | 批量处理回调函数（不能为 null） |

**异常**：
- `IllegalArgumentException`: 参数无效时抛出

### 方法详细说明

#### add(T message)

添加单条消息到聚合器。

| 参数 | 类型 | 说明 |
|------|------|------|
| message | T | 消息对象 |

**注意**：
- 达到数量阈值时自动触发批量处理
- 第一条消息添加时启动超时计时器
- 聚合器关闭后调用会抛出 `IllegalStateException`

#### addAll(List<T> messages)

批量添加消息。

| 参数 | 类型 | 说明 |
|------|------|------|
| messages | List<T> | 消息列表 |

**注意**：
- 可能触发多次批量处理
- null 或空列表会被忽略

#### flush()

强制刷新缓冲区，立即处理所有已聚合的消息。

**注意**：
- 不等待阈值或超时
- 空缓冲区调用无效果

#### shutdown()

关闭聚合器。

**处理流程**：
1. 刷新剩余消息
2. 关闭调度器
3. 等待最多 5 秒完成

**注意**：
- 关闭后不能再添加消息
- 应在应用关闭时调用（如 `@PreDestroy`）

#### getBufferSize()

获取当前缓冲区大小。

| 返回值 | 类型 | 说明 |
|--------|------|------|
| int | 缓冲区中的消息数量 |

### 线程安全

`MessageAggregator` 是线程安全的，使用 `ReentrantLock` 保护缓冲区操作。

### 使用示例

```java
MessageAggregator<String> aggregator = new MessageAggregator<>(
    100,     // 累积 100 条触发
    10000,   // 或 10 秒后触发
    messages -> {
        kafkaProducerService.sendBatchAsync("log-topic", messages);
    }
);

// 添加消息
aggregator.add("log message 1");
aggregator.add("log message 2");

// 获取缓冲区大小
int size = aggregator.getBufferSize();

// 强制刷新
aggregator.flush();

// 关闭
aggregator.shutdown();
```

---

## BatchSendResult

批量发送结果封装类。

### 类定义

```java
@Data
@Builder
public class BatchSendResult {
    private int total;
    private int successCount;
    private int failureCount;
    private List<SendResult<String, String>> successResults;
    private List<FailedMessage> failedMessages;

    public boolean isAllSuccess();
    public boolean isAllFailed();
    public boolean isPartialSuccess();

    @Data
    @Builder
    public static class FailedMessage {
        private int index;
        private String key;
        private String value;
        private String errorMessage;
        private Throwable exception;
    }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| total | int | 发送总数 |
| successCount | int | 成功数量 |
| failureCount | int | 失败数量 |
| successResults | List<SendResult> | 成功的发送结果列表 |
| failedMessages | List<FailedMessage> | 失败的消息列表 |

### 方法说明

#### isAllSuccess()

检查是否全部成功。

| 返回值 | 说明 |
|--------|------|
| true | failureCount == 0 |
| false | 有失败消息 |

#### isAllFailed()

检查是否全部失败。

| 返回值 | 说明 |
|--------|------|
| true | successCount == 0 && total > 0 |
| false | 有成功消息或列表为空 |

#### isPartialSuccess()

检查是否部分成功。

| 返回值 | 说明 |
|--------|------|
| true | successCount > 0 && failureCount > 0 |
| false | 全部成功或全部失败 |

### FailedMessage 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| index | int | 消息在批次中的索引 |
| key | String | 消息 key（可能为 null） |
| value | String | 消息内容 |
| errorMessage | String | 错误信息 |
| exception | Throwable | 异常对象 |

### 使用示例

```java
BatchSendResult result = kafkaProducerService.sendBatchSync(topic, messages, timeout);

if (result.isAllSuccess()) {
    log.info("全部发送成功: {}", result.getTotal());
} else if (result.isPartialSuccess()) {
    log.warn("部分发送成功: {}/{}", result.getSuccessCount(), result.getTotal());

    // 处理失败消息
    for (BatchSendResult.FailedMessage failed : result.getFailedMessages()) {
        log.error("失败消息[{}]: key={}, error={}",
            failed.getIndex(), failed.getKey(), failed.getErrorMessage());

        // 重试或记录
        retryOrLog(failed);
    }
} else {
    log.error("全部发送失败");
}
```

---

## 配置属性

### KafkaProperties 完整配置

```yaml
smart:
  kafka:
    # 基础配置
    enabled: false                        # 是否启用 Kafka
    bootstrap-servers: localhost:9092     # Kafka 集群地址

    # 生产者配置
    producer:
      acks: all                           # 确认模式: all/1/0
      retries: 3                          # 失败重试次数
      enable-idempotence: true            # 启用幂等性
      max-in-flight-requests-per-connection: 1  # 顺序保证
      batch-size: 16384                   # 批量发送大小（字节）
      buffer-memory: 33554432             # 缓冲区内存（字节）
      linger-ms: 5                        # 发送延迟（毫秒）

    # 消费者配置
    consumer:
      group-id: smart-admin-group         # 消费者组ID
      enable-auto-commit: false           # 禁用自动提交
      auto-offset-reset: earliest         # 偏移量重置策略
      max-poll-records: 500               # 单次拉取最大记录数
      max-poll-interval-ms: 300000        # 最大拉取间隔（毫秒）
      session-timeout-ms: 45000           # 会话超时（毫秒）
      heartbeat-interval-ms: 3000         # 心跳间隔（毫秒）

    # 监听器配置
    listener:
      ack-mode: MANUAL_IMMEDIATE          # 确认模式
      concurrency: 3                      # 并发消费者数量

    # 死信队列配置
    dead-letter-queue:
      enabled: true                       # 启用 DLQ
      topic-suffix: .dlq                  # DLQ topic 后缀
      retry:
        max-attempts: 3                   # 最大重试次数
        initial-interval: 1000            # 初始重试间隔（毫秒）
        max-interval: 10000               # 最大重试间隔（毫秒）
        multiplier: 2.0                   # 退避乘数

    # 批量处理配置
    batch:
      enabled: false                      # 启用批量消费模式
      size: 100                           # 批量消费大小
      concurrency: 3                      # 批量监听器并发数
      aggregate:
        enabled: false                    # 启用消息聚合
        count: 50                         # 聚合数量阈值
        timeout-ms: 5000                  # 聚合超时时间（毫秒）
```

### 配置项详细说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `smart.kafka.enabled` | false | 是否启用 Kafka 功能 |
| `smart.kafka.bootstrap-servers` | localhost:9092 | Kafka 集群地址 |
| `smart.kafka.producer.acks` | all | 确认模式，all 表示所有副本确认 |
| `smart.kafka.producer.retries` | 3 | 发送失败重试次数 |
| `smart.kafka.producer.enable-idempotence` | true | 启用幂等性，防止重复发送 |
| `smart.kafka.consumer.enable-auto-commit` | false | 建议关闭，使用手动确认 |
| `smart.kafka.batch.enabled` | false | 启用批量消费模式 |
| `smart.kafka.batch.size` | 100 | 批量消费的最大消息数 |
| `smart.kafka.dead-letter-queue.enabled` | true | 启用死信队列 |

---

## 相关链接

- [快速开始指南](kafka-batch-quickstart.md)
- [示例代码](kafka-batch-examples.md)
- [测试指南](kafka-batch-testing-guide.md)
