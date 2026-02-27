package net.lab1024.sa.igaming.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Form for player login.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerLoginForm {

  @Schema(description = "Username")
  @NotBlank(message = "username cannot be blank")
  private String username;

  @Schema(description = "Password")
  @NotBlank(message = "password cannot be blank")
  private String password;
}
