package net.lab1024.sa.common.mq.kafka.batch;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.kafka.support.SendResult;

/**
 * 批量发送结果封装
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Data
@Builder
public class BatchSendResult {

  /** 发送总数 */
  private int total;

  /** 成功数量 */
  private int successCount;

  /** 失败数量 */
  private int failureCount;

  /** 成功的发送结果列表 */
  private List<SendResult<String, String>> successResults;

  /** 失败的消息列表 */
  private List<FailedMessage> failedMessages;

  /**
   * 检查是否全部成功
   *
   * @return true 表示全部发送成功
   */
  public boolean isAllSuccess() {
    return failureCount == 0;
  }

  /**
   * 检查是否全部失败
   *
   * @return true 表示全部发送失败
   */
  public boolean isAllFailed() {
    return successCount == 0 && total > 0;
  }

  /**
   * 检查是否部分成功
   *
   * @return true 表示部分成功部分失败
   */
  public boolean isPartialSuccess() {
    return successCount > 0 && failureCount > 0;
  }

  /** 失败消息封装 */
  @Data
  @Builder
  public static class FailedMessage {

    /** 消息在批次中的索引 */
    private int index;

    /** 消息 key */
    private String key;

    /** 消息内容 */
    private String value;

    /** 错误信息 */
    private String errorMessage;

    /** 异常对象 */
    private Throwable exception;
  }
}
