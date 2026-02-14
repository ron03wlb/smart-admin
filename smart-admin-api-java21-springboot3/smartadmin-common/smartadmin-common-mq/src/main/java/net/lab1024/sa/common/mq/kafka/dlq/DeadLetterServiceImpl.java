package net.lab1024.sa.common.mq.kafka.dlq;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.config.KafkaProperties;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 死信队列服务实现
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "smart.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {

  private final KafkaProducerService kafkaProducerService;
  private final KafkaProperties kafkaProperties;

  @Override
  public void sendToDeadLetter(ConsumerRecord<String, String> record, Exception ex) {
    String dlqTopic = record.topic() + kafkaProperties.getDeadLetterQueue().getTopicSuffix();

    DeadLetterMessage message =
        DeadLetterMessage.builder()
            .originalTopic(record.topic())
            .partition(record.partition())
            .offset(record.offset())
            .key(record.key())
            .value(record.value())
            .exceptionMessage(ex.getMessage())
            .exceptionStackTrace(getStackTrace(ex))
            .failedAt(LocalDateTime.now())
            .retryCount(kafkaProperties.getDeadLetterQueue().getRetry().getMaxAttempts())
            .build();

    // 发送到 DLQ topic
    kafkaProducerService.sendAsync(dlqTopic, record.key(), record.value());

    // 记录日志
    logDeadLetter(message);
  }

  @Override
  public void logDeadLetter(DeadLetterMessage message) {
    log.error(
        "消息进入死信队列: topic={}, partition={}, offset={}, key={}, exception={}",
        message.getOriginalTopic(),
        message.getPartition(),
        message.getOffset(),
        message.getKey(),
        message.getExceptionMessage());

    log.debug("死信消息详情: value={}", message.getValue());
    log.debug("异常堆栈: {}", message.getExceptionStackTrace());
  }

  private String getStackTrace(Exception ex) {
    StringWriter sw = new StringWriter();
    try (PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
    }
    return sw.toString();
  }
}
