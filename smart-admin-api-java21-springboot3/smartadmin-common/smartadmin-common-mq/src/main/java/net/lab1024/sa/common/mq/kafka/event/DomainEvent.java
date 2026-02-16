package net.lab1024.sa.common.mq.kafka.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain event base class for iGaming platform
 *
 * <p>All business events must extend or use this class. Events are immutable after creation.
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

  /** Unique event identifier (UUID v4) */
  @Builder.Default private String eventId = UUID.randomUUID().toString();

  /** Event type (e.g., WALLET_DEBITED, PLAYER_REGISTERED) */
  private String eventType;

  /** Tenant identifier for multi-tenant isolation */
  private Long tenantId;

  /** Aggregate type (e.g., Wallet, Player, Risk) */
  private String aggregateType;

  /** Aggregate identifier (e.g., playerId, walletId) */
  private String aggregateId;

  /** Event payload as JSON */
  private JsonNode payload;

  /** Distributed trace identifier (from MDC) */
  private String traceId;

  /** Event timestamp (UTC) */
  @Builder.Default private Instant timestamp = Instant.now();

  /** Event schema version for backward compatibility */
  @Builder.Default private Integer version = 1;
}
