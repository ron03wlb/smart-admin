package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for adding a new game provider.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameProviderAddForm {

  @Schema(description = "Provider code (e.g., pgsoft, pragmatic, mock)")
  @NotBlank(message = "providerCode cannot be blank")
  @Size(max = 32)
  private String providerCode;

  @Schema(description = "Provider name")
  @NotBlank(message = "providerName cannot be blank")
  @Size(max = 128)
  private String providerName;

  @Schema(description = "API URL")
  @NotBlank(message = "apiUrl cannot be blank")
  @Size(max = 512)
  private String apiUrl;

  @Schema(description = "API key (plaintext, will be encrypted before storage)")
  @Size(max = 512)
  private String apiKey;

  @Schema(description = "Callback URL")
  @Size(max = 512)
  private String callbackUrl;
}
