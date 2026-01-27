package net.lab1024.sa.foundation.mq.kafka.core;

import io.vavr.control.Option;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    return future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error(
                "Kafka 消息发送失败: topic={}, key={}, errorType={}, error={}",
                topic,
                key,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex);
          } else if (log.isDebugEnabled()) {
            log.debug(
                "Kafka 消息发送成功: topic={}, key={}, partition={}, offset={}",
                topic,
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }

  @Override
  public Option<SendResult<String, String>> sendSync(String topic, String message) {
    return sendSync(topic, null, message, DEFAULT_TIMEOUT_MS);
  }

  @Override
  public Option<SendResult<String, String>> sendSync(String topic, String key, String message) {
    return sendSync(topic, key, message, DEFAULT_TIMEOUT_MS);
  }

  @Override
  public Option<SendResult<String, String>> sendSync(
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
      return Option.of(result);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Kafka 消息同步发送被中断: topic={}, key={}", topic, key, ex);
      return Option.none();
    } catch (TimeoutException ex) {
      log.error("Kafka 消息同步发送超时: topic={}, key={}, timeoutMs={}", topic, key, timeoutMs, ex);
      return Option.none();
    } catch (ExecutionException ex) {
      // ExecutionException 包装了实际的发送异常，需要解包
      Throwable cause = ex.getCause();
      log.error(
          "Kafka 消息同步发送失败 (broker错误): topic={}, key={}, causeType={}, error={}",
          topic,
          key,
          cause != null ? cause.getClass().getSimpleName() : "Unknown",
          cause != null ? cause.getMessage() : ex.getMessage(),
          cause != null ? cause : ex);
      return Option.none();
    } catch (Exception ex) {
      log.error(
          "Kafka 消息同步发送失败 (未知错误): topic={}, key={}, errorType={}, error={}",
          topic,
          key,
          ex.getClass().getSimpleName(),
          ex.getMessage(),
          ex);
      return Option.none();
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
                  // 解包 CompletionException 获取真实异常
                  Throwable cause = e.getCause();
                  Throwable actualException = (cause != null) ? cause : e;

                  String errorMessage =
                      String.format(
                          "[%s] %s",
                          actualException.getClass().getSimpleName(), actualException.getMessage());

                  failedMessages.add(
                      FailedMessage.builder()
                          .index(i)
                          .key(originalMessage.getKey())
                          .value(originalMessage.getValue())
                          .errorMessage(errorMessage)
                          .exception((Exception) actualException)
                          .build());
                  failureCount++;

                  if (log.isDebugEnabled()) {
                    log.debug(
                        "Kafka 批量发送单条消息失败: topic={}, index={}, key={}, errorType={}",
                        topic,
                        i,
                        originalMessage.getKey(),
                        actualException.getClass().getSimpleName(),
                        actualException);
                  }
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
      log.error("Kafka 批量同步发送被中断: topic={}, total={}", topic, keyedMessages.size(), ex);
      return createFailedBatchResult(keyedMessages, "发送被中断: " + ex.getMessage(), ex);
    } catch (TimeoutException ex) {
      log.error(
          "Kafka 批量同步发送超时: topic={}, total={}, timeoutMs={}",
          topic,
          keyedMessages.size(),
          timeoutMs,
          ex);
      return createFailedBatchResult(
          keyedMessages, "发送超时 (" + timeoutMs + "ms): " + ex.getMessage(), ex);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      log.error(
          "Kafka 批量同步发送失败 (broker错误): topic={}, total={}, causeType={}, error={}",
          topic,
          keyedMessages.size(),
          cause != null ? cause.getClass().getSimpleName() : "Unknown",
          cause != null ? cause.getMessage() : ex.getMessage(),
          cause != null ? cause : ex);
      return createFailedBatchResult(
          keyedMessages,
          "broker错误: " + (cause != null ? cause.getMessage() : ex.getMessage()),
          cause != null ? (Exception) cause : ex);
    } catch (Exception ex) {
      log.error(
          "Kafka 批量同步发送失败 (未知错误): topic={}, total={}, errorType={}, error={}",
          topic,
          keyedMessages.size(),
          ex.getClass().getSimpleName(),
          ex.getMessage(),
          ex);
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
