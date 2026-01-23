package net.lab1024.sa.admin.module.business.sample.kafka;

import jakarta.annotation.Resource;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.mq.kafka.constant.KafkaConst;
import net.lab1024.sa.foundation.mq.kafka.core.KafkaProducerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Kafka 生产者示例
 *
 * <p>展示如何使用 KafkaProducerService 发送消息
 *
 * <p>使用方式:
 *
 * <pre>{@code
 * @Resource
 * private KafkaProducerSample kafkaProducerSample;
 *
 * // 异步发送（推荐用于非关键业务）
 * kafkaProducerSample.sendSampleMessageAsync("业务数据");
 *
 * // 异步发送带 key（保证顺序）
 * kafkaProducerSample.sendOrderMessageAsync("ORDER-001", "订单数据");
 *
 * // 同步发送（关键业务，需要确认发送成功）
 * boolean success = kafkaProducerSample.sendSampleMessageSync("重要业务数据");
 * }</pre>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaProducerService.class)
public class KafkaProducerSample {

  @Resource private KafkaProducerService kafkaProducerService;

  /**
   * 异步发送示例消息
   *
   * @param message 消息内容
   */
  public void sendSampleMessageAsync(String message) {
    kafkaProducerService
        .sendAsync(KafkaConst.Topic.SAMPLE, message)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("示例消息发送失败: {}", ex.getMessage());
              } else {
                log.info(
                    "示例消息发送成功: partition={}, offset={}",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
              }
            });
  }

  /**
   * 异步发送订单消息（带 key 保证顺序）
   *
   * <p>相同 orderId 的消息会路由到同一分区，保证处理顺序
   *
   * @param orderId 订单ID（作为消息 key）
   * @param message 消息内容
   */
  public void sendOrderMessageAsync(String orderId, String message) {
    kafkaProducerService
        .sendAsync(KafkaConst.Topic.ORDER, orderId, message)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("订单消息发送失败: orderId={}, error={}", orderId, ex.getMessage());
              } else {
                log.info(
                    "订单消息发送成功: orderId={}, partition={}, offset={}",
                    orderId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
              }
            });
  }

  /**
   * 同步发送示例消息
   *
   * @param message 消息内容
   * @return 是否发送成功
   */
  public boolean sendSampleMessageSync(String message) {
    Optional<SendResult<String, String>> result =
        kafkaProducerService.sendSync(KafkaConst.Topic.SAMPLE, message);

    if (result.isPresent()) {
      log.info(
          "示例消息同步发送成功: partition={}, offset={}",
          result.get().getRecordMetadata().partition(),
          result.get().getRecordMetadata().offset());
      return true;
    } else {
      log.error("示例消息同步发送失败");
      return false;
    }
  }

  /**
   * 同步发送订单消息（带 key 保证顺序）
   *
   * @param orderId 订单ID（作为消息 key）
   * @param message 消息内容
   * @return 是否发送成功
   */
  public boolean sendOrderMessageSync(String orderId, String message) {
    Optional<SendResult<String, String>> result =
        kafkaProducerService.sendSync(KafkaConst.Topic.ORDER, orderId, message);

    if (result.isPresent()) {
      log.info(
          "订单消息同步发送成功: orderId={}, partition={}, offset={}",
          orderId,
          result.get().getRecordMetadata().partition(),
          result.get().getRecordMetadata().offset());
      return true;
    } else {
      log.error("订单消息同步发送失败: orderId={}", orderId);
      return false;
    }
  }
}
