package net.lab1024.sa.common.mq.kafka.core;

import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.lab1024.sa.common.mq.kafka.batch.BatchSendResult;
import org.springframework.kafka.support.SendResult;

/**
 * Kafka 生产者服务接口
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
public interface KafkaProducerService {

  /**
   * 异步发送消息（无 key）
   *
   * @param topic Topic 名称
   * @param message 消息内容
   * @return 发送结果的 CompletableFuture
   */
  CompletableFuture<SendResult<String, String>> sendAsync(String topic, String message);

  /**
   * 异步发送消息（带 key，保证顺序）
   *
   * @param topic Topic 名称
   * @param key 消息 Key（相同 key 会路由到同一分区，保证顺序）
   * @param message 消息内容
   * @return 发送结果的 CompletableFuture
   */
  CompletableFuture<SendResult<String, String>> sendAsync(String topic, String key, String message);

  /**
   * 同步发送消息（无 key）
   *
   * @param topic Topic 名称
   * @param message 消息内容
   * @return 发送结果，发送失败返回 Option.none()
   */
  Option<SendResult<String, String>> sendSync(String topic, String message);

  /**
   * 同步发送消息（带 key，保证顺序）
   *
   * @param topic Topic 名称
   * @param key 消息 Key（相同 key 会路由到同一分区，保证顺序）
   * @param message 消息内容
   * @return 发送结果，发送失败返回 Option.none()
   */
  Option<SendResult<String, String>> sendSync(String topic, String key, String message);

  /**
   * 同步发送消息（带 key，保证顺序，指定超时）
   *
   * @param topic Topic 名称
   * @param key 消息 Key
   * @param message 消息内容
   * @param timeoutMs 超时时间（毫秒）
   * @return 发送结果，发送失败返回 Option.none()
   */
  Option<SendResult<String, String>> sendSync(
      String topic, String key, String message, long timeoutMs);

  /**
   * 异步批量发送消息（无 key）
   *
   * @param topic Topic 名称
   * @param messages 消息列表
   * @return 批量发送结果的 CompletableFuture
   */
  CompletableFuture<BatchSendResult> sendBatchAsync(String topic, List<String> messages);

  /**
   * 异步批量发送消息（带 key）
   *
   * <p>使用 Map.Entry 传递 key-value 对，相同 key 的消息会路由到同一分区
   *
   * @param topic Topic 名称
   * @param keyedMessages key-value 消息列表
   * @return 批量发送结果的 CompletableFuture
   */
  CompletableFuture<BatchSendResult> sendBatchAsyncWithKeys(
      String topic, List<Map.Entry<String, String>> keyedMessages);

  /**
   * 同步批量发送消息（无 key）
   *
   * @param topic Topic 名称
   * @param messages 消息列表
   * @param timeoutMs 超时时间（毫秒）
   * @return 批量发送结果
   */
  BatchSendResult sendBatchSync(String topic, List<String> messages, long timeoutMs);

  /**
   * 同步批量发送消息（带 key）
   *
   * @param topic Topic 名称
   * @param keyedMessages key-value 消息列表
   * @param timeoutMs 超时时间（毫秒）
   * @return 批量发送结果
   */
  BatchSendResult sendBatchSyncWithKeys(
      String topic, List<Map.Entry<String, String>> keyedMessages, long timeoutMs);
}
