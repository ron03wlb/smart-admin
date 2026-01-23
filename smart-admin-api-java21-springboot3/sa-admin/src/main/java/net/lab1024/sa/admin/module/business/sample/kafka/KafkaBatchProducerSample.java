package net.lab1024.sa.admin.module.business.sample.kafka;

import jakarta.annotation.Resource;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.mq.kafka.batch.BatchSendResult;
import net.lab1024.sa.foundation.mq.kafka.batch.BatchSendResult.FailedMessage;
import net.lab1024.sa.foundation.mq.kafka.constant.KafkaConst;
import net.lab1024.sa.foundation.mq.kafka.core.KafkaProducerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Kafka 批量生产者示例
 *
 * <p>展示如何使用 KafkaProducerService 进行批量消息发送
 *
 * <p>使用方式:
 *
 * <pre>{@code
 * @Resource
 * private KafkaBatchProducerSample kafkaBatchProducerSample;
 *
 * // 异步批量发送
 * kafkaBatchProducerSample.sendBatchMessagesAsync(List.of("msg1", "msg2", "msg3"));
 *
 * // 异步批量发送带 key（保证相同 key 的消息顺序）
 * kafkaBatchProducerSample.sendBatchOrderMessagesAsync(List.of(
 *     new AbstractMap.SimpleEntry<>("ORDER-001", "订单数据1"),
 *     new AbstractMap.SimpleEntry<>("ORDER-001", "订单数据2"),
 *     new AbstractMap.SimpleEntry<>("ORDER-002", "订单数据3")
 * ));
 *
 * // 同步批量发送（需要确认发送结果）
 * BatchSendResult result = kafkaBatchProducerSample.sendBatchMessagesSync(messages);
 * if (!result.isAllSuccess()) {
 *     // 处理失败消息
 * }
 * }</pre>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaProducerService.class)
public class KafkaBatchProducerSample {

  /** 默认同步发送超时时间（毫秒） */
  private static final long DEFAULT_TIMEOUT_MS = 30000;

  @Resource private KafkaProducerService kafkaProducerService;

  /**
   * 异步批量发送示例消息
   *
   * @param messages 消息列表
   */
  public void sendBatchMessagesAsync(List<String> messages) {
    kafkaProducerService
        .sendBatchAsync(KafkaConst.Topic.SAMPLE, messages)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("批量发送异常: {}", ex.getMessage(), ex);
                return;
              }
              handleBatchResult("异步批量发送", result);
            });
  }

  /**
   * 异步批量发送订单消息（带 key 保证顺序）
   *
   * <p>相同 orderId 的消息会路由到同一分区，保证处理顺序
   *
   * @param orderMessages key-value 消息列表（key 为订单ID）
   */
  public void sendBatchOrderMessagesAsync(List<Map.Entry<String, String>> orderMessages) {
    kafkaProducerService
        .sendBatchAsyncWithKeys(KafkaConst.Topic.ORDER, orderMessages)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("批量订单消息发送异常: {}", ex.getMessage(), ex);
                return;
              }
              handleBatchResult("异步批量订单发送", result);
            });
  }

  /**
   * 同步批量发送示例消息
   *
   * @param messages 消息列表
   * @return 批量发送结果
   */
  public BatchSendResult sendBatchMessagesSync(List<String> messages) {
    BatchSendResult result =
        kafkaProducerService.sendBatchSync(KafkaConst.Topic.SAMPLE, messages, DEFAULT_TIMEOUT_MS);
    handleBatchResult("同步批量发送", result);
    return result;
  }

  /**
   * 同步批量发送订单消息（带 key 保证顺序）
   *
   * @param orderMessages key-value 消息列表
   * @return 批量发送结果
   */
  public BatchSendResult sendBatchOrderMessagesSync(List<Map.Entry<String, String>> orderMessages) {
    BatchSendResult result =
        kafkaProducerService.sendBatchSyncWithKeys(
            KafkaConst.Topic.ORDER, orderMessages, DEFAULT_TIMEOUT_MS);
    handleBatchResult("同步批量订单发送", result);
    return result;
  }

  /**
   * 生成测试消息示例
   *
   * @param count 消息数量
   * @return 消息列表
   */
  public List<String> generateTestMessages(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> String.format("{\"id\":%d,\"content\":\"测试消息%d\"}", i, i))
        .collect(Collectors.toList());
  }

  /**
   * 生成测试订单消息示例（带 key）
   *
   * @param orderCount 订单数量
   * @param messagesPerOrder 每个订单的消息数量
   * @return key-value 消息列表
   */
  public List<Map.Entry<String, String>> generateTestOrderMessages(
      int orderCount, int messagesPerOrder) {
    return IntStream.range(0, orderCount)
        .boxed()
        .flatMap(
            orderId ->
                IntStream.range(0, messagesPerOrder)
                    .mapToObj(
                        msgIdx ->
                            new AbstractMap.SimpleEntry<>(
                                "ORDER-" + orderId,
                                String.format(
                                    "{\"orderId\":%d,\"step\":%d,\"action\":\"处理步骤%d\"}",
                                    orderId, msgIdx, msgIdx))))
        .collect(Collectors.toList());
  }

  /**
   * 处理批量发送结果
   *
   * @param operation 操作名称
   * @param result 发送结果
   */
  private void handleBatchResult(String operation, BatchSendResult result) {
    if (result.isAllSuccess()) {
      log.info("{} 成功: total={}", operation, result.getTotal());
    } else if (result.isPartialSuccess()) {
      log.warn(
          "{} 部分成功: total={}, success={}, failed={}",
          operation,
          result.getTotal(),
          result.getSuccessCount(),
          result.getFailureCount());
      logFailedMessages(result.getFailedMessages());
    } else {
      log.error("{} 全部失败: total={}", operation, result.getTotal());
      logFailedMessages(result.getFailedMessages());
    }
  }

  /**
   * 记录失败消息详情
   *
   * @param failedMessages 失败消息列表
   */
  private void logFailedMessages(List<FailedMessage> failedMessages) {
    for (FailedMessage msg : failedMessages) {
      log.error(
          "失败消息详情: index={}, key={}, error={}",
          msg.getIndex(),
          msg.getKey(),
          msg.getErrorMessage());
    }
  }
}
