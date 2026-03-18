package net.lab1024.sa.igaming.integration.payment.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for PSP deposit callback.
 *
 * <p>This form captures PSP (Payment Service Provider) deposit callback data, including order
 * identification, PSP transaction ID, and signature verification payload.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Data
public class DepositCallbackForm {

  @Schema(description = "Platform order number")
  @NotBlank(message = "orderNo cannot be blank")
  @Size(max = 64)
  private String orderNo;

  @Schema(description = "PSP transaction ID")
  @NotBlank(message = "pspTransactionId cannot be blank")
  @Size(max = 128)
  private String pspTransactionId;

  @Schema(description = "PSP callback signature for verification")
  @NotBlank(message = "signature cannot be blank")
  @Size(max = 256)
  private String signature;

  @Schema(description = "Raw callback payload (JSON string for audit trail)")
  @Size(max = 4000)
  private String callbackPayload;

  @Schema(description = "PSP code (e.g., stripe, paypal)")
  @Size(max = 30)
  private String pspCode;
}
