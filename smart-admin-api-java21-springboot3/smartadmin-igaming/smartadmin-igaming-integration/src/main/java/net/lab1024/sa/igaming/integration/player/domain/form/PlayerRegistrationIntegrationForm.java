package net.lab1024.sa.igaming.integration.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for integrated player registration with wallet creation.
 *
 * <p>This form combines player registration with automatic wallet setup, streamlining the
 * onboarding process by creating both player account and CASH/BONUS wallets in a single
 * orchestrated flow.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class PlayerRegistrationIntegrationForm {

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

  @Schema(description = "Currency code (ISO 4217), default USD")
  @Size(min = 3, max = 3, message = "currencyCode must be 3 characters")
  @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be uppercase ISO 4217 format")
  private String currencyCode = "USD";

  @Schema(description = "Referral code (optional)")
  @Size(max = 50)
  private String referralCode;
}
