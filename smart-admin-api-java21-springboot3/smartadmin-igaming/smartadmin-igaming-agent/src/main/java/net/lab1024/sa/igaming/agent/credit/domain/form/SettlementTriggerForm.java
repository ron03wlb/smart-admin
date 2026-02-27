package net.lab1024.sa.igaming.agent.credit.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Form for triggering weekly settlement.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class SettlementTriggerForm {

  @Schema(description = "Tenant ID")
  @NotNull(message = "tenantId cannot be null")
  private Long tenantId;

  @Schema(description = "Settlement week (e.g. 2026-W09)")
  @NotBlank(message = "settlementWeek cannot be blank")
  private String settlementWeek;
}
