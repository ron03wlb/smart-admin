package net.lab1024.sa.igaming.risk.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Deserialized risk event from Kafka — maps from DomainEvent payload.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskEvent {

  private Long playerId;

  private Long tenantId;

  private String eventType;

  private String eventId;

  private BigDecimal amount;

  private BigDecimal playerBalance;

  private String deviceId;

  private String ipAddress;

  private String countryCode;

  private String gameType;

  private String gameCode;
}
