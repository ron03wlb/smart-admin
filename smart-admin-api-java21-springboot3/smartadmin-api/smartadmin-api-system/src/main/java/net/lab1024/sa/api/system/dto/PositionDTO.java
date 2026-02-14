package net.lab1024.sa.api.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Position Data Transfer Object
 *
 * <p>API contract DTO for position information transfer between modules.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Position DTO")
public class PositionDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Position ID")
  @NotNull(
      groups = {Update.class},
      message = "Position ID cannot be null")
  private Long positionId;

  @Schema(description = "Position name")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Position name cannot be blank")
  @Size(max = 50, message = "Position name length cannot exceed 50")
  private String positionName;

  @Schema(description = "Position level")
  @Size(max = 20, message = "Position level length cannot exceed 20")
  private String positionLevel;

  @Schema(description = "Sort order")
  @Min(value = 0, message = "Sort order cannot be negative")
  private Integer sort;

  @Schema(description = "Remark")
  @Size(max = 200, message = "Remark length cannot exceed 200")
  private String remark;

  @Schema(description = "Create time")
  private OffsetDateTime createTime;

  @Schema(description = "Update time")
  private OffsetDateTime updateTime;

  /** Validation group for create operations */
  public interface Create {}

  /** Validation group for update operations */
  public interface Update {}
}
