package net.lab1024.sa.api.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Employee Data Transfer Object
 *
 * <p>API contract DTO for employee information transfer between modules. Designed for: - Internal
 * method calls in monolithic architecture - Future Feign/RPC remote calls in microservices
 * architecture
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee DTO")
public class EmployeeDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Employee ID")
  @NotNull(
      groups = {Update.class},
      message = "Employee ID cannot be null")
  private Long employeeId;

  @Schema(description = "Login name")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Login name cannot be blank")
  @Size(min = 3, max = 30, message = "Login name length must be between 3 and 30")
  @Pattern(
      regexp = "^[a-zA-Z0-9_]+$",
      message = "Login name can only contain letters, numbers, and underscores")
  private String loginName;

  @Schema(description = "Gender (1: Male, 2: Female, 3: Unknown)")
  private Integer gender;

  @Schema(description = "Actual name")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Actual name cannot be blank")
  @Size(max = 50, message = "Actual name length cannot exceed 50")
  private String actualName;

  @Schema(description = "Phone number")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
  private String phone;

  @Schema(description = "Department ID")
  @NotNull(
      groups = {Create.class, Update.class},
      message = "Department ID cannot be null")
  private Long departmentId;

  @Schema(description = "Department name (read-only)")
  private String departmentName;

  @Schema(description = "Disabled flag (true: disabled, false: enabled)")
  private Boolean disabledFlag;

  @Schema(description = "Administrator flag (true: admin, false: normal user)")
  private Boolean administratorFlag;

  @Schema(description = "Position ID")
  private Long positionId;

  @Schema(description = "Position name (read-only)")
  private String positionName;

  @Schema(description = "Email address")
  @Email(message = "Invalid email format")
  @Size(max = 100, message = "Email length cannot exceed 100")
  private String email;

  @Schema(description = "Role ID list")
  private List<Long> roleIdList;

  @Schema(description = "Role name list (read-only)")
  private List<String> roleNameList;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;

  /** Validation group for create operations */
  public interface Create {}

  /** Validation group for update operations */
  public interface Update {}
}
