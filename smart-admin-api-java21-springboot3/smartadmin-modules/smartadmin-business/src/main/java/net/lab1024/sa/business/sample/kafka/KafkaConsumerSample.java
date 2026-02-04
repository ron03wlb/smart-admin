package net.lab1024.sa.business.sample.kafka;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.constant.KafkaConst;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import net.lab1024.sa.common.mq.kafka.listener.AbstractKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者示例
 *
 * <p>展示如何继承 AbstractKafkaListener 实现消息消费
 *
 * <p>特点: - 手动确认模式，确保消息不丢失 - 使用 AbstractKafkaListener 模板方法处理异常 - 支持多分区并发消费
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaProducerService.class)
public class KafkaConsumerSample extends AbstractKafkaListener<String> {

  /**
   * 监听示例 Topic
   *
   * <p>使用 containerFactory 指定监听器容器工厂
   *
   * @param record 消息记录
   * @param ack 确认对象
   */
  @KafkaListener(
      topics = KafkaConst.Topic.SAMPLE,
      groupId = KafkaConst.Group.SAMPLE,
      containerFactory = "kafkaListenerContainerFactory")
  public void onSampleMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
    handleMessage(record, ack);
  }

  /**
   * 业务处理逻辑
   *
   * @param record 消息记录
   */
  @Override
  protected void doHandle(ConsumerRecord<String, String> record) {
    String message = record.value();
    String key = record.key();

    log.info("收到示例消息: key={}, message={}", key, message);

    // TODO: 在此处实现业务逻辑
    // 例如：解析消息、调用业务服务、更新数据库等
    processSampleMessage(key, message);
  }

  /**
   * 处理示例消息的业务逻辑
   *
   * @param key 消息 key
   * @param message 消息内容
   */
  private void processSampleMessage(String key, String message) {
    // 示例业务处理
    log.debug("处理示例消息: key={}, message={}", key, message);
  }

  /**
   * 重写异常处理（可选）
   *
   * <p>可以在此实现自定义异常处理，如发送到死信队列
   *
   * @param record 消息记录
   * @param ex 异常
   */
  @Override
  protected void handleException(ConsumerRecord<String, String> record, Exception ex) {
    // 调用父类默认处理（记录日志）
    super.handleException(record, ex);

    // TODO: 可以在此实现自定义异常处理
    // 例如：发送到死信队列、告警通知等
    // deadLetterService.send(record, ex);
  }
}
