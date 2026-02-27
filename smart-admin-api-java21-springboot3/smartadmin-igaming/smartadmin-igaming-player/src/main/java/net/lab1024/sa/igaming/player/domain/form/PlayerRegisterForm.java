package net.lab1024.sa.igaming.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for player registration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerRegisterForm {

  @Schema(description = "Username")
  @NotBlank(message = "username cannot be blank")
  @Size(min = 3, max = 50, message = "username must be 3-50 characters")
  private String username;

  @Schema(description = "Password")
  @NotBlank(message = "password cannot be blank")
  @Size(min = 8, max = 128, message = "password must be 8-128 characters")
  private String password;

  @Schema(description = "Email address")
  @Size(max = 200)
  private String email;

  @Schema(description = "Phone number")
  @Size(max = 30)
  private String phone;

  @Schema(description = "Registration IP address")
  @Size(max = 45)
  private String registrationIp;
}
