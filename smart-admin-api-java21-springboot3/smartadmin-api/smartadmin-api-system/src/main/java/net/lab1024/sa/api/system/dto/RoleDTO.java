package net.lab1024.sa.api.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Data Transfer Object
 *
 * <p>API contract DTO for role information transfer between modules.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role DTO")
public class RoleDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Role ID")
  @NotNull(
      groups = {Update.class},
      message = "Role ID cannot be null")
  private Long roleId;

  @Schema(description = "Role name")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Role name cannot be blank")
  @Size(max = 50, message = "Role name length cannot exceed 50")
  private String roleName;

  @Schema(description = "Role code")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Role code cannot be blank")
  @Size(max = 50, message = "Role code length cannot exceed 50")
  @Pattern(
      regexp = "^[A-Z_]+$",
      message = "Role code can only contain uppercase letters and underscores")
  private String roleCode;

  @Schema(description = "Role remark")
  @Size(max = 200, message = "Remark length cannot exceed 200")
  private String remark;

  /** Validation group for create operations */
  public interface Create {}

  /** Validation group for update operations */
  public interface Update {}
}
