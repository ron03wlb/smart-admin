package net.lab1024.sa.igaming.game.domain.dto;

import lombok.Data;

/**
 * Rollback result DTO from GP adapter.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RollbackResult {

  private String originalTransactionId;
  private String status;
}
