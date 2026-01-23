package net.lab1024.sa.foundation.mq.kafka.dlq;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 死信消息实体
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Data
@Builder
public class DeadLetterMessage {

  /** 原始 topic */
  private String originalTopic;

  /** 分区号 */
  private Integer partition;

  /** 偏移量 */
  private Long offset;

  /** 消息 key */
  private String key;

  /** 消息值 */
  private String value;

  /** 异常信息 */
  private String exceptionMessage;

  /** 异常堆栈 */
  private String exceptionStackTrace;

  /** 失败时间 */
  private LocalDateTime failedAt;

  /** 重试次数 */
  private Integer retryCount;
}
