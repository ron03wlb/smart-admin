package net.lab1024.sa.igaming.game.domain.dto;

import lombok.Data;

/**
 * Rollback request DTO for GP adapter.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RollbackRequest {

  private Long playerId;
  private Long tenantId;
  private String originalTransactionId;
  private String roundId;
}
