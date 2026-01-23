package net.lab1024.sa.foundation.mq.kafka.dlq;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 死信队列服务接口
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
public interface DeadLetterService {

  /**
   * 发送消息到死信队列
   *
   * @param record 原始消费记录
   * @param ex 异常信息
   */
  void sendToDeadLetter(ConsumerRecord<String, String> record, Exception ex);

  /**
   * 记录死信消息（日志/数据库）
   *
   * @param message 死信消息
   */
  void logDeadLetter(DeadLetterMessage message);
}
