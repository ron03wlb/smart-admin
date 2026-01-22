# Kafka 批量处理与批量发送需求文档

## 1. 概述

为现有 Kafka 模块增添批量处理与批量发送能力，提升高吞吐场景下的消息处理效率。

## 2. 现有架构

### 2.1 模块位置
```
sa-common/mq/src/main/java/net/lab1024/sa/common/mq/kafka/
├── config/          # 配置层
├── constant/        # 常量定义
├── core/            # Producer 核心功能
├── listener/        # Consumer 监听器
└── dlq/             # 死信队列
```

### 2.2 现有能力
| 功能 | 现状 |
|-----|-----|
| Producer | 单条发送（同步/异步） |
| Consumer | 逐条处理，AbstractKafkaListener 模板方法 |
| 底层批量配置 | batch-size、linger-ms、max-poll-records |

## 3. 需求规格

### 3.1 Producer 批量发送

#### 3.1.1 功能要求
- 新增异步批量发送接口 `sendBatchAsync`
- 支持一次发送多条消息到同一 Topic
- 返回每条消息的发送结果

#### 3.1.2 接口设计
```java
public interface KafkaProducerService {
    // 现有接口保持不变...

    /**
     * 异步批量发送消息（无 key）
     * @param topic 目标 Topic
     * @param messages 消息列表
     * @return 每条消息的发送结果
     */
    CompletableFuture<List<SendResult<String, String>>> sendBatchAsync(
        String topic,
        List<String> messages
    );

    /**
     * 异步批量发送消息（带 key）
     * @param topic 目标 Topic
     * @param keyedMessages key-value 消息列表
     * @return 每条消息的发送结果
     */
    CompletableFuture<List<SendResult<String, String>>> sendBatchAsync(
        String topic,
        List<Map.Entry<String, String>> keyedMessages
    );
}
```

#### 3.1.3 实现要点
- 使用 `KafkaTemplate.send()` 并行发送所有消息
- 收集所有 `CompletableFuture` 并使用 `CompletableFuture.allOf()` 等待完成
- 返回包含所有发送结果的列表，保持与输入顺序一致
- 记录批量发送的起始、完成日志及失败详情

### 3.2 Consumer 批量处理

#### 3.2.1 功能要求
- 新增批量消费者抽象基类 `AbstractBatchKafkaListener`
- 支持一次接收多条消息进行批量处理
- 可配置单批次处理的消息数量上限
- 支持按数量聚合（累积 N 条消息后批量处理）

#### 3.2.2 接口设计
```java
public abstract class AbstractBatchKafkaListener<T> {

    /**
     * 批量消息处理入口
     * @param records 消息记录列表
     * @param ack 确认对象
     */
    public void handleBatch(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    );

    /**
     * 子类实现：批量处理业务逻辑
     * @param records 消息记录列表
     */
    protected abstract void doBatchHandle(
        List<ConsumerRecord<String, String>> records
    );

    /**
     * 子类实现：单条处理业务逻辑（降级时使用）
     * @param record 单条消息记录
     */
    protected abstract void doHandle(ConsumerRecord<String, String> record);

    /**
     * 获取批量大小配置
     * @return 单批次最大消息数量
     */
    protected int getBatchSize();
}
```

#### 3.2.3 配置项
```yaml
smart:
  kafka:
    batch:
      enabled: true
      size: 100              # 单批次最大消息数量
      aggregate:
        enabled: true
        count: 50            # 按数量聚合：累积 N 条后处理
        timeout-ms: 5000     # 聚合超时时间（防止消息堆积）
    listener:
      batch-listener: true   # 启用批量监听模式
```

#### 3.2.4 使用示例
```java
@Component
public class OrderBatchConsumer extends AbstractBatchKafkaListener<OrderMessage> {

    @KafkaListener(
        topics = KafkaConst.Topic.ORDER,
        groupId = KafkaConst.Group.ORDER,
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onOrderBatch(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment ack
    ) {
        handleBatch(records, ack);
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        // 批量业务处理逻辑
        List<OrderMessage> orders = records.stream()
            .map(r -> parseOrder(r.value()))
            .toList();
        orderService.batchProcess(orders);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        // 单条处理逻辑（降级时使用）
        OrderMessage order = parseOrder(record.value());
        orderService.process(order);
    }
}
```

### 3.3 错误处理策略

#### 3.3.1 逐条降级机制
当批量处理失败时，自动降级为逐条处理：

```
批量处理流程:
┌─────────────────────────────────────────────────────────┐
│  接收批量消息 (N 条)                                      │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  尝试批量处理 doBatchHandle()                            │
└─────────────────────────────────────────────────────────┘
                          │
            ┌─────────────┴─────────────┐
            │                           │
         成功                         失败
            │                           │
            ▼                           ▼
┌───────────────────┐     ┌───────────────────────────────┐
│  批量确认 ack()    │     │  降级：逐条处理 doHandle()     │
└───────────────────┘     └───────────────────────────────┘
                                        │
                          ┌─────────────┴─────────────┐
                          │                           │
                       单条成功                     单条失败
                          │                           │
                          ▼                           ▼
                    ┌───────────┐           ┌─────────────────┐
                    │  正常确认  │           │  发送到 DLQ     │
                    └───────────┘           └─────────────────┘
```

#### 3.3.2 实现要点
- 批量处理异常时，捕获异常并记录日志
- 遍历每条消息，单独调用 `doHandle()` 处理
- 单条处理成功的消息正常确认
- 单条处理失败的消息发送到 DLQ（复用现有 `DeadLetterService`）
- 所有消息处理完成后统一确认

## 4. 配置变更

### 4.1 KafkaProperties.java 新增配置
```java
@Data
public class KafkaProperties {
    // 现有配置保持不变...

    /**
     * 批量处理配置
     */
    private BatchProperties batch = new BatchProperties();

    @Data
    public static class BatchProperties {
        /**
         * 是否启用批量处理
         */
        private boolean enabled = false;

        /**
         * 单批次最大消息数量
         */
        private int size = 100;

        /**
         * 聚合配置
         */
        private AggregateProperties aggregate = new AggregateProperties();
    }

    @Data
    public static class AggregateProperties {
        /**
         * 是否启用按数量聚合
         */
        private boolean enabled = false;

        /**
         * 聚合数量阈值
         */
        private int count = 50;

        /**
         * 聚合超时时间（毫秒）
         */
        private long timeoutMs = 5000;
    }
}
```

### 4.2 KafkaAutoConfiguration.java 新增配置
- 新增 `BatchKafkaListenerContainerFactory` Bean
- 配置批量监听模式 `setBatchListener(true)`
- 配置批量错误处理器

## 5. 文件变更清单

| 文件路径 | 变更类型 | 说明 |
|---------|---------|-----|
| `core/KafkaProducerService.java` | 修改 | 新增 sendBatchAsync 方法 |
| `core/KafkaProducerServiceImpl.java` | 修改 | 实现批量发送逻辑 |
| `listener/AbstractBatchKafkaListener.java` | 新增 | 批量消费者抽象基类 |
| `config/KafkaProperties.java` | 修改 | 新增批量相关配置项 |
| `config/KafkaAutoConfiguration.java` | 修改 | 新增批量 ContainerFactory |
| `config/BatchKafkaListenerContainerFactory.java` | 新增（可选） | 批量监听器工厂配置 |

## 6. 依赖要求

无需新增依赖，使用现有 Spring Kafka 的批量监听能力：
- `spring-kafka` 已支持 `setBatchListener(true)`
- `@KafkaListener` 已支持接收 `List<ConsumerRecord>`

## 7. 测试要点

### 7.1 Producer 测试
- [ ] 批量发送成功，返回所有成功结果
- [ ] 部分发送失败，返回混合结果
- [ ] 空列表发送，返回空结果
- [ ] 大批量发送性能测试

### 7.2 Consumer 测试
- [ ] 批量处理成功，所有消息确认
- [ ] 批量处理失败，自动降级为逐条处理
- [ ] 逐条处理部分失败，失败消息发送到 DLQ
- [ ] 聚合功能：累积达到阈值后触发批量处理
- [ ] 聚合超时：未达阈值但超时后触发处理

## 8. 版本计划

| 版本 | 内容 |
|-----|-----|
| v1.0 | Producer 批量发送 + Consumer 批量处理基础功能 |
| v1.1 | 按数量聚合功能 |
| v1.2 | 监控指标（批量处理耗时、降级次数等） |
