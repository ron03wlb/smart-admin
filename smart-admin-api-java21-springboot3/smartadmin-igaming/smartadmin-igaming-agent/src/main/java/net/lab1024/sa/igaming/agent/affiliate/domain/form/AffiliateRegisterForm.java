package net.lab1024.sa.igaming.agent.affiliate.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for registering a new affiliate agent.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class AffiliateRegisterForm {

  @Schema(description = "Agent username")
  @NotBlank(message = "username cannot be blank")
  @Size(max = 64, message = "username max 64 characters")
  private String username;

  @Schema(description = "Parent agent ID (null for top-level)")
  private Long parentAgentId;

  @Schema(description = "Commission plan ID")
  private Long commissionPlanId;

  @Schema(description = "Referral code")
  @Size(max = 32, message = "referralCode max 32 characters")
  private String referralCode;

  @Schema(description = "Tenant ID")
  @NotNull(message = "tenantId cannot be null")
  private Long tenantId;
}
