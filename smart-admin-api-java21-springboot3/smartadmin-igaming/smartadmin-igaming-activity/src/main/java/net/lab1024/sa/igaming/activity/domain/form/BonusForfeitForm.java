package net.lab1024.sa.igaming.activity.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for forfeiting an active bonus record.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class BonusForfeitForm {

  @Schema(description = "Bonus record ID")
  @NotNull
  private Long recordId;

  @Schema(description = "Reason for forfeiture")
  @NotBlank
  @Size(max = 256)
  private String reason;
}
