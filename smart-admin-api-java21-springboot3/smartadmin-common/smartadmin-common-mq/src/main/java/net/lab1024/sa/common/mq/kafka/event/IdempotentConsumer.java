package net.lab1024.sa.common.mq.kafka.event;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.listener.AbstractKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Idempotent consumer base class implementing ADR-015 three-layer defense
 *
 * <p>Layer 1: Redis SETNX (fast duplicate check, < 5ms)
 *
 * <p>Layer 2: Redisson distributed lock (concurrent protection)
 *
 * <p>Layer 3: DB t_idempotent_key UNIQUE constraint (final safety net)
 *
 * @param <T> message type
 * @author iGaming Team
 * @since 2026-02-14
 * @see <a href="../architecture/adr/ADR-015_Idempotency_Three_Layer_Defense.md">ADR-015</a>
 */
@Slf4j
public abstract class IdempotentConsumer<T> extends AbstractKafkaListener<T> {

  private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:event:";
  private static final Duration IDEMPOTENT_TTL = Duration.ofHours(1);
  private static final long LOCK_WAIT_SECONDS = 5;
  private static final long LOCK_LEASE_SECONDS = 30;

  private final StringRedisTemplate redisTemplate;
  private final JdbcTemplate jdbcTemplate;
  private final RedissonClient redissonClient;

  protected IdempotentConsumer(
      StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate, RedissonClient redissonClient) {
    this.redisTemplate = redisTemplate;
    this.jdbcTemplate = jdbcTemplate;
    this.redissonClient = redissonClient;
  }

  @Override
  protected void doHandle(ConsumerRecord<String, String> record) {
    DomainEvent event = JsonUtil.fromJson(record.value(), DomainEvent.class);
    String eventId = event.getEventId();

    // === Layer 1: Redis SETNX fast duplicate check ===
    String redisKey = IDEMPOTENT_KEY_PREFIX + eventId;
    Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", IDEMPOTENT_TTL);

    if (Boolean.FALSE.equals(isNew)) {
      log.info("Event duplicate detected at Layer 1 (Redis): eventId={}", eventId);
      return;
    }

    // === Layer 2: Distributed lock for concurrent protection ===
    String lockKey = "lock:event:" + eventId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      boolean acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
      if (!acquired) {
        log.warn("Failed to acquire lock for event: eventId={}, will retry via Kafka", eventId);
        throw new EventProcessingException(
            "Failed to acquire distributed lock for event: " + eventId);
      }

      try {
        // === Layer 3: DB UNIQUE constraint as final safety net ===
        insertIdempotentKey(eventId, event.getEventType());

        // Delegate to subclass business logic
        processEvent(event);

        log.debug(
            "Event processed successfully: eventId={}, eventType={}",
            eventId,
            event.getEventType());

      } catch (DuplicateKeyException e) {
        log.info("Event duplicate detected at Layer 3 (DB): eventId={}", eventId);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EventProcessingException("Lock acquisition interrupted for event: " + eventId, e);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  /**
   * Insert idempotent key into database
   *
   * @param eventId unique event identifier
   * @param eventType event type for auditing
   */
  private void insertIdempotentKey(String eventId, String eventType) {
    jdbcTemplate.update(
        "INSERT INTO t_idempotent_key (event_id, event_type, created_at) VALUES (?, ?, NOW())",
        eventId,
        eventType);
  }

  /**
   * Process the domain event (subclass implementation)
   *
   * @param event the domain event to process
   */
  protected abstract void processEvent(DomainEvent event);
}
