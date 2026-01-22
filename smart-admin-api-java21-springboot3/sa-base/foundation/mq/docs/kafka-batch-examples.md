# Kafka Batch Examples

本文档提供 SmartAdmin Kafka 批量功能的实际业务场景示例代码。

## 目录

- [场景一：订单批量发送](#场景一订单批量发送)
- [场景二：日志批量消费](#场景二日志批量消费)
- [场景三：高频埋点数据聚合](#场景三高频埋点数据聚合)
- [场景四：错误处理和 DLQ 处理](#场景四错误处理和-dlq-处理)
- [场景五：带重试的批量发送](#场景五带重试的批量发送)

---

## 场景一：订单批量发送

### 业务场景

电商系统在大促期间，需要批量处理订单状态变更通知。

### 完整示例

```java
package net.lab1024.sa.admin.module.business.order.service;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.batch.BatchSendResult;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.springframework.stereotype.Service;

/**
 * 订单消息服务
 *
 * <p>负责发送订单相关的 Kafka 消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMessageService {

  private static final String TOPIC_ORDER_STATUS = "order-status-change";
  private static final long SEND_TIMEOUT_MS = 30000;

  private final KafkaProducerService kafkaProducerService;
  private final ObjectMapper objectMapper;

  /**
   * 批量发送订单状态变更消息
   *
   * @param statusChanges 状态变更列表
   * @return 发送结果
   */
  public BatchSendResult sendOrderStatusChanges(List<OrderStatusChangeDTO> statusChanges) {
    if (statusChanges == null || statusChanges.isEmpty()) {
      log.debug("订单状态变更列表为空，跳过发送");
      return createEmptyResult();
    }

    // 转换为带 key 的消息（使用订单ID作为 key 保证同一订单的消息顺序）
    List<Map.Entry<String, String>> keyedMessages = statusChanges.stream()
        .map(change -> {
          try {
            String key = change.getOrderId();
            String value = objectMapper.writeValueAsString(change);
            return new AbstractMap.SimpleEntry<>(key, value);
          } catch (Exception e) {
            log.error("序列化订单状态变更失败: orderId={}", change.getOrderId(), e);
            return null;
          }
        })
        .filter(entry -> entry != null)
        .collect(Collectors.toList());

    // 同步发送（确保消息发送成功后再返回）
    BatchSendResult result = kafkaProducerService.sendBatchSyncWithKeys(
        TOPIC_ORDER_STATUS,
        keyedMessages,
        SEND_TIMEOUT_MS
    );

    // 日志记录
    logSendResult(result);

    return result;
  }

  /**
   * 异步批量发送订单创建通知
   *
   * @param orders 订单列表
   */
  public void sendOrderCreatedAsync(List<OrderVO> orders) {
    List<String> messages = orders.stream()
        .map(this::toJson)
        .filter(json -> json != null)
        .collect(Collectors.toList());

    kafkaProducerService.sendBatchAsync("order-created", messages)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("订单创建通知发送异常: count={}, error={}",
                orders.size(), ex.getMessage(), ex);
            // 可以记录到失败表，后续重试
            saveFailedMessages(orders, ex);
            return;
          }

          if (result.isAllSuccess()) {
            log.info("订单创建通知全部发送成功: count={}", result.getTotal());
          } else {
            log.warn("订单创建通知部分发送失败: success={}, failed={}",
                result.getSuccessCount(), result.getFailureCount());
            // 处理失败的消息
            handleFailedMessages(result.getFailedMessages());
          }
        });
  }

  private void logSendResult(BatchSendResult result) {
    if (result.isAllSuccess()) {
      log.info("订单状态变更消息全部发送成功: total={}", result.getTotal());
    } else if (result.isPartialSuccess()) {
      log.warn("订单状态变更消息部分发送成功: total={}, success={}, failed={}",
          result.getTotal(), result.getSuccessCount(), result.getFailureCount());
    } else {
      log.error("订单状态变更消息全部发送失败: total={}", result.getTotal());
    }
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      log.error("JSON序列化失败", e);
      return null;
    }
  }

  private BatchSendResult createEmptyResult() {
    return BatchSendResult.builder()
        .total(0)
        .successCount(0)
        .failureCount(0)
        .successResults(List.of())
        .failedMessages(List.of())
        .build();
  }

  private void saveFailedMessages(List<OrderVO> orders, Throwable ex) {
    // 实现：保存到数据库，后续重试
  }

  private void handleFailedMessages(List<BatchSendResult.FailedMessage> failedMessages) {
    // 实现：记录失败详情，发送告警等
  }
}
```

---

## 场景二：日志批量消费

### 业务场景

从 Kafka 批量消费应用日志，写入 Elasticsearch 进行分析。

### 完整示例

```java
package net.lab1024.sa.admin.module.system.log.listener;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.system.log.service.LogElasticsearchService;
import net.lab1024.sa.common.mq.kafka.listener.AbstractBatchKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 应用日志批量消费者
 *
 * <p>从 Kafka 批量消费日志并写入 Elasticsearch
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart.kafka.batch", name = "enabled", havingValue = "true")
public class AppLogBatchListener extends AbstractBatchKafkaListener<AppLogDTO> {

  /** 批量写入 ES 的最大大小 */
  private static final int ES_BULK_SIZE = 500;

  private final LogElasticsearchService logElasticsearchService;
  private final ObjectMapper objectMapper;

  /**
   * 批量监听应用日志
   */
  @KafkaListener(
      topics = "app-log",
      groupId = "log-es-writer",
      containerFactory = "batchKafkaListenerContainerFactory",
      // 自定义属性覆盖
      properties = {
          "max.poll.records=1000",
          "max.poll.interval.ms=600000"
      })
  public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
    handleBatch(records, ack);
  }

  /**
   * 批量处理：将日志批量写入 ES
   */
  @Override
  protected void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception {
    log.info("开始批量处理日志: batchSize={}", records.size());
    long startTime = System.currentTimeMillis();

    // 解析日志
    List<AppLogDTO> logs = records.stream()
        .map(this::parseLog)
        .filter(logDto -> logDto != null)
        .collect(Collectors.toList());

    if (logs.isEmpty()) {
      log.warn("日志解析后为空，跳过写入");
      return;
    }

    // 分批写入 ES（避免单次写入过多）
    int batches = (logs.size() + ES_BULK_SIZE - 1) / ES_BULK_SIZE;
    for (int i = 0; i < batches; i++) {
      int from = i * ES_BULK_SIZE;
      int to = Math.min(from + ES_BULK_SIZE, logs.size());
      List<AppLogDTO> batch = logs.subList(from, to);

      logElasticsearchService.bulkInsert(batch);
      log.debug("ES 批量写入完成: batch={}/{}, size={}", i + 1, batches, batch.size());
    }

    long elapsed = System.currentTimeMillis() - startTime;
    log.info("日志批量处理完成: total={}, elapsed={}ms, throughput={} logs/s",
        logs.size(), elapsed, logs.size() * 1000 / Math.max(elapsed, 1));
  }

  /**
   * 单条处理：降级时逐条写入 ES
   */
  @Override
  protected void doHandle(ConsumerRecord<String, String> record) throws Exception {
    AppLogDTO logDto = parseLog(record);
    if (logDto == null) {
      throw new RuntimeException("日志解析失败: offset=" + record.offset());
    }

    logElasticsearchService.insert(logDto);
    log.debug("单条日志写入完成: traceId={}", logDto.getTraceId());
  }

  /**
   * 解析日志消息
   */
  private AppLogDTO parseLog(ConsumerRecord<String, String> record) {
    try {
      AppLogDTO logDto = objectMapper.readValue(record.value(), AppLogDTO.class);
      // 补充 Kafka 元数据
      logDto.setKafkaPartition(record.partition());
      logDto.setKafkaOffset(record.offset());
      logDto.setKafkaTimestamp(record.timestamp());
      return logDto;
    } catch (Exception e) {
      log.error("日志解析失败: partition={}, offset={}, error={}",
          record.partition(), record.offset(), e.getMessage());
      return null;
    }
  }
}
```

---

## 场景三：高频埋点数据聚合

### 业务场景

前端埋点数据高频上报，需要聚合后批量发送到 Kafka。

### 完整示例

```java
package net.lab1024.sa.admin.module.business.tracking.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.batch.MessageAggregator;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 埋点数据聚合服务
 *
 * <p>高频埋点数据先聚合，再批量发送到 Kafka
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart.kafka.batch.aggregate", name = "enabled", havingValue = "true")
public class TrackingAggregateService {

  private static final String TOPIC_TRACKING = "user-tracking";

  /** 聚合数量阈值 */
  private static final int AGGREGATE_COUNT = 200;

  /** 聚合超时时间（毫秒） */
  private static final long AGGREGATE_TIMEOUT_MS = 3000;

  private final KafkaProducerService kafkaProducerService;
  private final ObjectMapper objectMapper;

  private MessageAggregator<TrackingEvent> aggregator;

  /** 统计计数器 */
  private final AtomicLong totalReceived = new AtomicLong(0);
  private final AtomicLong totalSent = new AtomicLong(0);
  private final AtomicLong totalFailed = new AtomicLong(0);

  @PostConstruct
  public void init() {
    aggregator = new MessageAggregator<>(
        AGGREGATE_COUNT,
        AGGREGATE_TIMEOUT_MS,
        this::handleBatch
    );

    log.info("埋点聚合器初始化完成: countThreshold={}, timeoutMs={}",
        AGGREGATE_COUNT, AGGREGATE_TIMEOUT_MS);
  }

  @PreDestroy
  public void destroy() {
    if (aggregator != null) {
      log.info("关闭埋点聚合器，统计: received={}, sent={}, failed={}",
          totalReceived.get(), totalSent.get(), totalFailed.get());
      aggregator.shutdown();
    }
  }

  /**
   * 接收埋点事件
   *
   * <p>该方法会被高频调用，通过聚合器缓冲后批量发送
   *
   * @param event 埋点事件
   */
  public void track(TrackingEvent event) {
    totalReceived.incrementAndGet();
    aggregator.add(event);
  }

  /**
   * 批量接收埋点事件
   *
   * @param events 埋点事件列表
   */
  public void trackBatch(List<TrackingEvent> events) {
    totalReceived.addAndGet(events.size());
    aggregator.addAll(events);
  }

  /**
   * 获取当前缓冲区大小
   */
  public int getBufferSize() {
    return aggregator.getBufferSize();
  }

  /**
   * 获取统计信息
   */
  public TrackingStats getStats() {
    return new TrackingStats(
        totalReceived.get(),
        totalSent.get(),
        totalFailed.get(),
        getBufferSize()
    );
  }

  /**
   * 强制刷新（用于测试或优雅关闭）
   */
  public void flush() {
    aggregator.flush();
  }

  /**
   * 批量处理回调
   */
  private void handleBatch(List<TrackingEvent> events) {
    log.debug("聚合器触发批量处理: count={}", events.size());

    // 转换为 JSON
    List<String> messages = events.stream()
        .map(this::toJson)
        .filter(json -> json != null)
        .toList();

    if (messages.isEmpty()) {
      return;
    }

    // 异步发送
    kafkaProducerService.sendBatchAsync(TOPIC_TRACKING, messages)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("埋点消息发送异常: count={}, error={}",
                events.size(), ex.getMessage(), ex);
            totalFailed.addAndGet(events.size());
            return;
          }

          totalSent.addAndGet(result.getSuccessCount());
          totalFailed.addAndGet(result.getFailureCount());

          if (result.isPartialSuccess()) {
            log.warn("埋点消息部分发送失败: success={}, failed={}",
                result.getSuccessCount(), result.getFailureCount());
          }
        });
  }

  private String toJson(TrackingEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (Exception e) {
      log.error("埋点事件序列化失败: eventType={}", event.getEventType(), e);
      return null;
    }
  }

  /**
   * 统计信息
   */
  public record TrackingStats(
      long totalReceived,
      long totalSent,
      long totalFailed,
      int bufferSize
  ) {}
}
```

### 使用示例

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tracking")
public class TrackingController {

  private final TrackingAggregateService trackingService;

  /**
   * 上报埋点（高频调用）
   */
  @PostMapping("/event")
  public ResponseDTO<Void> track(@RequestBody TrackingEvent event) {
    trackingService.track(event);
    return ResponseDTO.ok();
  }

  /**
   * 批量上报埋点
   */
  @PostMapping("/events")
  public ResponseDTO<Void> trackBatch(@RequestBody List<TrackingEvent> events) {
    trackingService.trackBatch(events);
    return ResponseDTO.ok();
  }

  /**
   * 获取统计信息（运维接口）
   */
  @GetMapping("/stats")
  public ResponseDTO<TrackingAggregateService.TrackingStats> getStats() {
    return ResponseDTO.ok(trackingService.getStats());
  }
}
```

---

## 场景四：错误处理和 DLQ 处理

### 业务场景

消费失败的消息需要发送到死信队列，并提供管理界面进行查看和重试。

### DLQ 消息处理服务

```java
package net.lab1024.sa.admin.module.system.dlq.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.system.dlq.dao.DlqMessageDao;
import net.lab1024.sa.admin.module.system.dlq.domain.entity.DlqMessageEntity;
import net.lab1024.sa.admin.module.system.dlq.domain.form.DlqRetryForm;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * 死信队列处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqProcessingService {

  private final DlqMessageDao dlqMessageDao;
  private final KafkaProducerService kafkaProducerService;
  private final AlertService alertService;

  /**
   * 监听订单死信队列
   */
  @KafkaListener(
      topics = "order-topic.dlq",
      groupId = "dlq-processor")
  public void handleOrderDlq(ConsumerRecord<String, String> record, Acknowledgment ack) {
    try {
      // 保存到数据库
      DlqMessageEntity entity = new DlqMessageEntity();
      entity.setOriginalTopic(extractOriginalTopic(record.topic()));
      entity.setMessageKey(record.key());
      entity.setMessageValue(record.value());
      entity.setPartition(record.partition());
      entity.setOffset(record.offset());
      entity.setTimestamp(record.timestamp());
      entity.setCreateTime(LocalDateTime.now());
      entity.setStatus("PENDING");
      entity.setRetryCount(0);

      // 提取错误信息（从 header 中）
      String errorMessage = extractErrorMessage(record);
      entity.setErrorMessage(errorMessage);

      dlqMessageDao.insert(entity);

      // 发送告警
      alertService.sendAlert(
          "订单消息处理失败",
          String.format("topic=%s, key=%s, error=%s",
              entity.getOriginalTopic(),
              entity.getMessageKey(),
              errorMessage)
      );

      log.info("DLQ 消息已保存: id={}, topic={}, key={}",
          entity.getId(), entity.getOriginalTopic(), entity.getMessageKey());

      ack.acknowledge();
    } catch (Exception e) {
      log.error("处理 DLQ 消息失败", e);
      // 不确认，让消息重试
    }
  }

  /**
   * 重试 DLQ 消息
   */
  public ResponseDTO<Void> retry(DlqRetryForm form) {
    DlqMessageEntity entity = dlqMessageDao.selectById(form.getId());
    if (entity == null) {
      return ResponseDTO.userErrorParam("DLQ 消息不存在");
    }

    if (entity.getRetryCount() >= 3) {
      return ResponseDTO.userErrorParam("重试次数已达上限");
    }

    // 重新发送到原始 topic
    kafkaProducerService.sendAsync(
        entity.getOriginalTopic(),
        entity.getMessageKey(),
        entity.getMessageValue()
    ).whenComplete((result, ex) -> {
      if (ex != null) {
        log.error("DLQ 消息重试失败: id={}", entity.getId(), ex);
        entity.setStatus("RETRY_FAILED");
        entity.setRetryCount(entity.getRetryCount() + 1);
      } else {
        log.info("DLQ 消息重试成功: id={}", entity.getId());
        entity.setStatus("RETRIED");
      }
      entity.setLastRetryTime(LocalDateTime.now());
      dlqMessageDao.updateById(entity);
    });

    return ResponseDTO.ok();
  }

  /**
   * 批量重试
   */
  public ResponseDTO<Integer> batchRetry(List<Long> ids) {
    int successCount = 0;
    for (Long id : ids) {
      DlqRetryForm form = new DlqRetryForm();
      form.setId(id);
      if (retry(form).getOk()) {
        successCount++;
      }
    }
    return ResponseDTO.ok(successCount);
  }

  /**
   * 忽略 DLQ 消息（标记为已处理）
   */
  public ResponseDTO<Void> ignore(Long id) {
    DlqMessageEntity entity = dlqMessageDao.selectById(id);
    if (entity == null) {
      return ResponseDTO.userErrorParam("DLQ 消息不存在");
    }

    entity.setStatus("IGNORED");
    dlqMessageDao.updateById(entity);
    return ResponseDTO.ok();
  }

  private String extractOriginalTopic(String dlqTopic) {
    // "order-topic.dlq" -> "order-topic"
    if (dlqTopic.endsWith(".dlq")) {
      return dlqTopic.substring(0, dlqTopic.length() - 4);
    }
    return dlqTopic;
  }

  private String extractErrorMessage(ConsumerRecord<String, String> record) {
    // 从 Kafka header 中提取错误信息
    if (record.headers() != null) {
      var header = record.headers().lastHeader("kafka_dlt-exception-message");
      if (header != null) {
        return new String(header.value());
      }
    }
    return "Unknown error";
  }
}
```

---

## 场景五：带重试的批量发送

### 业务场景

关键业务消息发送失败时需要自动重试。

### 完整示例

```java
package net.lab1024.sa.admin.module.business.message.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.batch.BatchSendResult;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.springframework.stereotype.Service;

/**
 * 带重试的消息发送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableMessageService {

  private static final int MAX_RETRY_ATTEMPTS = 3;
  private static final long RETRY_DELAY_MS = 1000;
  private static final long SEND_TIMEOUT_MS = 10000;

  private final KafkaProducerService kafkaProducerService;

  /**
   * 带重试的批量发送
   *
   * @param topic 主题
   * @param messages 消息列表
   * @return 最终发送结果
   */
  public BatchSendResult sendWithRetry(String topic, List<String> messages) {
    List<String> pendingMessages = new ArrayList<>(messages);
    List<BatchSendResult.FailedMessage> allFailedMessages = new ArrayList<>();
    int totalSuccess = 0;

    for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS && !pendingMessages.isEmpty(); attempt++) {
      log.info("批量发送尝试: attempt={}/{}, pendingCount={}",
          attempt, MAX_RETRY_ATTEMPTS, pendingMessages.size());

      BatchSendResult result = kafkaProducerService.sendBatchSync(
          topic, pendingMessages, SEND_TIMEOUT_MS);

      totalSuccess += result.getSuccessCount();

      if (result.isAllSuccess()) {
        log.info("批量发送成功: attempt={}, count={}", attempt, result.getTotal());
        pendingMessages.clear();
        break;
      }

      // 收集失败的消息用于重试
      pendingMessages.clear();
      for (BatchSendResult.FailedMessage failed : result.getFailedMessages()) {
        if (attempt < MAX_RETRY_ATTEMPTS) {
          // 还有重试机会，加入待发送列表
          pendingMessages.add(failed.getValue());
        } else {
          // 已达最大重试次数，记录最终失败
          allFailedMessages.add(failed);
        }
      }

      if (!pendingMessages.isEmpty() && attempt < MAX_RETRY_ATTEMPTS) {
        // 等待后重试
        log.warn("批量发送部分失败，准备重试: failedCount={}, retryDelay={}ms",
            pendingMessages.size(), RETRY_DELAY_MS * attempt);
        sleep(RETRY_DELAY_MS * attempt);
      }
    }

    // 构建最终结果
    BatchSendResult finalResult = BatchSendResult.builder()
        .total(messages.size())
        .successCount(totalSuccess)
        .failureCount(allFailedMessages.size())
        .successResults(List.of()) // 简化，不保留成功详情
        .failedMessages(allFailedMessages)
        .build();

    if (!finalResult.isAllSuccess()) {
      log.error("批量发送最终结果: total={}, success={}, failed={}",
          messages.size(), totalSuccess, allFailedMessages.size());
    }

    return finalResult;
  }

  /**
   * 带指数退避的异步发送
   */
  public void sendAsyncWithBackoff(String topic, List<String> messages,
                                   SendResultCallback callback) {
    sendAsyncWithBackoffInternal(topic, messages, 1, callback);
  }

  private void sendAsyncWithBackoffInternal(String topic, List<String> messages,
                                            int attempt, SendResultCallback callback) {
    kafkaProducerService.sendBatchAsync(topic, messages)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            handleAsyncError(topic, messages, attempt, ex, callback);
            return;
          }

          if (result.isAllSuccess()) {
            callback.onSuccess(result);
            return;
          }

          if (attempt >= MAX_RETRY_ATTEMPTS) {
            callback.onFailure(result);
            return;
          }

          // 提取失败消息重试
          List<String> failedMessages = result.getFailedMessages().stream()
              .map(BatchSendResult.FailedMessage::getValue)
              .toList();

          long delay = calculateBackoffDelay(attempt);
          log.warn("批量发送部分失败，{}ms 后重试: attempt={}, failedCount={}",
              delay, attempt, failedMessages.size());

          // 延迟后重试
          scheduler.schedule(
              () -> sendAsyncWithBackoffInternal(topic, failedMessages, attempt + 1, callback),
              delay,
              TimeUnit.MILLISECONDS
          );
        });
  }

  private void handleAsyncError(String topic, List<String> messages,
                                int attempt, Throwable ex, SendResultCallback callback) {
    if (attempt >= MAX_RETRY_ATTEMPTS) {
      log.error("批量发送最终失败: topic={}, count={}", topic, messages.size(), ex);
      callback.onError(ex);
      return;
    }

    long delay = calculateBackoffDelay(attempt);
    log.warn("批量发送异常，{}ms 后重试: attempt={}, error={}",
        delay, attempt, ex.getMessage());

    scheduler.schedule(
        () -> sendAsyncWithBackoffInternal(topic, messages, attempt + 1, callback),
        delay,
        TimeUnit.MILLISECONDS
    );
  }

  private long calculateBackoffDelay(int attempt) {
    // 指数退避: 1s, 2s, 4s, ...
    return RETRY_DELAY_MS * (1L << (attempt - 1));
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * 发送结果回调接口
   */
  public interface SendResultCallback {
    void onSuccess(BatchSendResult result);
    void onFailure(BatchSendResult result);
    void onError(Throwable ex);
  }
}
```

---

## 总结

| 场景 | 推荐方案 | 关键点 |
|------|----------|--------|
| 订单批量发送 | `sendBatchSyncWithKeys` | 使用 key 保证顺序 |
| 日志批量消费 | `AbstractBatchKafkaListener` | 批量写入 ES |
| 高频埋点 | `MessageAggregator` | 聚合后批量发送 |
| 错误处理 | DLQ + 管理界面 | 自动告警 + 手动重试 |
| 关键消息 | 带重试的发送 | 指数退避重试 |

## 相关链接

- [快速开始指南](kafka-batch-quickstart.md)
- [API 参考文档](kafka-batch-api-reference.md)
- [测试指南](kafka-batch-testing-guide.md)
