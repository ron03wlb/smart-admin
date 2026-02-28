package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/**
 * Financial report query form.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Data
public class FinancialReportQueryForm {

  @Schema(description = "Report start date (inclusive)")
  @NotNull(message = "startDate cannot be null")
  private LocalDate startDate;

  @Schema(description = "Report end date (inclusive)")
  @NotNull(message = "endDate cannot be null")
  private LocalDate endDate;
}
