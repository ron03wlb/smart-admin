package net.lab1024.sa.common.mq.kafka.event;

/**
 * Exception thrown when event processing fails and the message should be retried by Kafka.
 *
 * <p>Throwing this exception prevents offset commit, allowing Kafka to redeliver the message.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
public class EventProcessingException extends RuntimeException {

  public EventProcessingException(String message) {
    super(message);
  }

  public EventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
