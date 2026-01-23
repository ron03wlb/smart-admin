package net.lab1024.sa.foundation.mq.kafka.core;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.mq.kafka.batch.BatchSendResult;
import net.lab1024.sa.foundation.mq.kafka.batch.BatchSendResult.FailedMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Kafka 生产者服务实现
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement") // SLF4J 占位符已优化性能
public class KafkaProducerServiceImpl implements KafkaProducerService {

  /** 默认同步发送超时时间（毫秒） */
  private static final long DEFAULT_TIMEOUT_MS = 10000;

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public CompletableFuture<SendResult<String, String>> sendAsync(String topic, String message) {
    return sendAsync(topic, null, message);
  }

  @Override
  public CompletableFuture<SendResult<String, String>> sendAsync(
      String topic, String key, String message) {
    CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error("Kafka 消息发送失败: topic={}, key={}, error={}", topic, key, ex.getMessage());
          } else if (log.isDebugEnabled()) {
            log.debug(
                "Kafka 消息发送成功: topic={}, key={}, partition={}, offset={}",
                topic,
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });

    return future;
  }

  @Override
  public Optional<SendResult<String, String>> sendSync(String topic, String message) {
    return sendSync(topic, null, message, DEFAULT_TIMEOUT_MS);
  }

  @Override
  public Optional<SendResult<String, String>> sendSync(String topic, String key, String message) {
    return sendSync(topic, key, message, DEFAULT_TIMEOUT_MS);
  }

  @Override
  public Optional<SendResult<String, String>> sendSync(
      String topic, String key, String message, long timeoutMs) {
    try {
      CompletableFuture<SendResult<String, String>> future =
          kafkaTemplate.send(topic, key, message);
      SendResult<String, String> result = future.get(timeoutMs, TimeUnit.MILLISECONDS);

      if (log.isDebugEnabled()) {
        log.debug(
            "Kafka 消息同步发送成功: topic={}, key={}, partition={}, offset={}",
            topic,
            key,
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
      }
      return Optional.of(result);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Kafka 消息同步发送被中断: topic={}, key={}", topic, key, ex);
      return Optional.empty();
    } catch (Exception ex) {
      log.error("Kafka 消息同步发送失败: topic={}, key={}, error={}", topic, key, ex.getMessage(), ex);
      return Optional.empty();
    }
  }

  @Override
  public CompletableFuture<BatchSendResult> sendBatchAsync(String topic, List<String> messages) {
    // 将无 key 消息转换为 key-value 对（key 为 null）
    List<Map.Entry<String, String>> keyedMessages = new ArrayList<>(messages.size());
    for (String message : messages) {
      keyedMessages.add(new AbstractMap.SimpleEntry<>(null, message));
    }
    return sendBatchAsyncWithKeys(topic, keyedMessages);
  }

  @Override
  public CompletableFuture<BatchSendResult> sendBatchAsyncWithKeys(
      String topic, List<Map.Entry<String, String>> keyedMessages) {

    if (keyedMessages == null || keyedMessages.isEmpty()) {
      return CompletableFuture.completedFuture(
          BatchSendResult.builder()
              .total(0)
              .successCount(0)
              .failureCount(0)
              .successResults(List.of())
              .failedMessages(List.of())
              .build());
    }

    int total = keyedMessages.size();
    List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>(total);
    List<Map.Entry<String, String>> messagesCopy = new ArrayList<>(keyedMessages);

    // 发送所有消息
    for (Map.Entry<String, String> entry : messagesCopy) {
      CompletableFuture<SendResult<String, String>> future =
          kafkaTemplate.send(topic, entry.getKey(), entry.getValue());
      futures.add(future);
    }

    // 等待所有发送完成并收集结果
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .handle(
            (ignored, ex) -> {
              List<SendResult<String, String>> successResults = new ArrayList<>();
              List<FailedMessage> failedMessages = new ArrayList<>();
              int successCount = 0;
              int failureCount = 0;

              for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<SendResult<String, String>> future = futures.get(i);
                Map.Entry<String, String> originalMessage = messagesCopy.get(i);

                try {
                  // getNow 不会阻塞，因为 allOf 已经完成
                  SendResult<String, String> result = future.getNow(null);
                  if (result != null) {
                    successResults.add(result);
                    successCount++;
                  } else {
                    // 理论上不应该发生
                    failedMessages.add(
                        FailedMessage.builder()
                            .index(i)
                            .key(originalMessage.getKey())
                            .value(originalMessage.getValue())
                            .errorMessage("发送结果为空")
                            .build());
                    failureCount++;
                  }
                } catch (Exception e) {
                  failedMessages.add(
                      FailedMessage.builder()
                          .index(i)
                          .key(originalMessage.getKey())
                          .value(originalMessage.getValue())
                          .errorMessage(e.getMessage())
                          .exception(e)
                          .build());
                  failureCount++;
                }
              }

              BatchSendResult result =
                  BatchSendResult.builder()
                      .total(total)
                      .successCount(successCount)
                      .failureCount(failureCount)
                      .successResults(successResults)
                      .failedMessages(failedMessages)
                      .build();

              if (result.isAllSuccess()) {
                log.info("Kafka 批量发送成功: topic={}, total={}", topic, total);
              } else if (result.isPartialSuccess()) {
                log.warn(
                    "Kafka 批量发送部分成功: topic={}, success={}, failed={}",
                    topic,
                    successCount,
                    failureCount);
              } else {
                log.error("Kafka 批量发送全部失败: topic={}, total={}", topic, total);
              }

              return result;
            });
  }

  @Override
  public BatchSendResult sendBatchSync(String topic, List<String> messages, long timeoutMs) {
    List<Map.Entry<String, String>> keyedMessages = new ArrayList<>(messages.size());
    for (String message : messages) {
      keyedMessages.add(new AbstractMap.SimpleEntry<>(null, message));
    }
    return sendBatchSyncWithKeys(topic, keyedMessages, timeoutMs);
  }

  @Override
  public BatchSendResult sendBatchSyncWithKeys(
      String topic, List<Map.Entry<String, String>> keyedMessages, long timeoutMs) {
    try {
      return sendBatchAsyncWithKeys(topic, keyedMessages).get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Kafka 批量同步发送被中断: topic={}", topic, ex);
      return createFailedBatchResult(keyedMessages, "发送被中断: " + ex.getMessage(), ex);
    } catch (Exception ex) {
      log.error("Kafka 批量同步发送失败: topic={}, error={}", topic, ex.getMessage(), ex);
      return createFailedBatchResult(keyedMessages, ex.getMessage(), ex);
    }
  }

  /**
   * 创建全部失败的批量发送结果
   *
   * @param keyedMessages 消息列表
   * @param errorMessage 错误信息
   * @param ex 异常
   * @return 失败的批量发送结果
   */
  private BatchSendResult createFailedBatchResult(
      List<Map.Entry<String, String>> keyedMessages, String errorMessage, Exception ex) {
    List<FailedMessage> failedMessages = new ArrayList<>(keyedMessages.size());
    for (int i = 0; i < keyedMessages.size(); i++) {
      Map.Entry<String, String> entry = keyedMessages.get(i);
      failedMessages.add(
          FailedMessage.builder()
              .index(i)
              .key(entry.getKey())
              .value(entry.getValue())
              .errorMessage(errorMessage)
              .exception(ex)
              .build());
    }
    return BatchSendResult.builder()
        .total(keyedMessages.size())
        .successCount(0)
        .failureCount(keyedMessages.size())
        .successResults(List.of())
        .failedMessages(failedMessages)
        .build();
  }
}
