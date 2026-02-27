package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for updating an existing game provider.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameProviderUpdateForm {

  @Schema(description = "Provider ID")
  @NotNull(message = "providerId cannot be null")
  private Long providerId;

  @Schema(description = "Provider name")
  @Size(max = 128)
  private String providerName;

  @Schema(description = "API URL")
  @Size(max = 512)
  private String apiUrl;

  @Schema(description = "API key (plaintext, will be re-encrypted if provided)")
  @Size(max = 512)
  private String apiKey;

  @Schema(description = "Callback URL")
  @Size(max = 512)
  private String callbackUrl;

  @Schema(description = "Enabled flag")
  private Boolean enabled;
}
