package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/**
 * Form for triggering daily reconciliation.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class ReconciliationTriggerForm {

  @Schema(description = "Reconciliation date")
  @NotNull(message = "date cannot be null")
  private LocalDate date;
}
