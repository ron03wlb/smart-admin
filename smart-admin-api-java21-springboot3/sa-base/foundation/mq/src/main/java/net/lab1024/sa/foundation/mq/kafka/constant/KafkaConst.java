package net.lab1024.sa.foundation.mq.kafka.constant;

import lombok.NoArgsConstructor;

/**
 * Kafka Topic 和 Group 常量定义
 *
 * <p>业务模块应在此定义 Topic 和 Consumer Group 常量
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@NoArgsConstructor
public final class KafkaConst {

  /** Topic 常量 */
  @NoArgsConstructor
  public static final class Topic {

    /** 示例 Topic */
    public static final String SAMPLE = "smart-admin-sample";

    /** 订单 Topic */
    public static final String ORDER = "smart-admin-order";

    /** 用户 Topic */
    public static final String USER = "smart-admin-user";

    /** 通知消息 Topic */
    public static final String NOTIFICATION = "smart-admin-notification";
  }

  /** Consumer Group 常量 */
  @NoArgsConstructor
  public static final class Group {

    /** 示例消费者组 */
    public static final String SAMPLE = "smart-admin-sample-group";

    /** 订单消费者组 */
    public static final String ORDER = "smart-admin-order-group";

    /** 用户消费者组 */
    public static final String USER = "smart-admin-user-group";

    /** 通知消费者组 */
    public static final String NOTIFICATION = "smart-admin-notification-group";
  }
}
