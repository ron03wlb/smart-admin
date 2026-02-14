package net.lab1024.sa.api.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Department Data Transfer Object
 *
 * <p>API contract DTO for department information transfer between modules.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Department DTO")
public class DepartmentDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Department ID")
  @NotNull(
      groups = {Update.class},
      message = "Department ID cannot be null")
  private Long departmentId;

  @Schema(description = "Department name")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Department name cannot be blank")
  @Size(max = 50, message = "Department name length cannot exceed 50")
  private String departmentName;

  @Schema(description = "Manager name (read-only)")
  private String managerName;

  @Schema(description = "Manager ID")
  private Long managerId;

  @Schema(description = "Parent department ID")
  @NotNull(
      groups = {Create.class, Update.class},
      message = "Parent ID cannot be null")
  private Long parentId;

  @Schema(description = "Sort order")
  @Min(value = 0, message = "Sort order cannot be negative")
  private Integer sort;

  @Schema(description = "Update time")
  private OffsetDateTime updateTime;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;

  /** Validation group for create operations */
  public interface Create {}

  /** Validation group for update operations */
  public interface Update {}
}
