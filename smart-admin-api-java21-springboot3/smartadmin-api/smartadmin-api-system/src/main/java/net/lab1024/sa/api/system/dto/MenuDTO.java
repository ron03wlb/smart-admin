package net.lab1024.sa.api.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Menu Data Transfer Object
 *
 * <p>API contract DTO for menu information transfer between modules. Simplified version containing
 * only core fields needed for cross-module API calls.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Menu DTO")
public class MenuDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Menu ID")
  @NotNull(
      groups = {Update.class},
      message = "Menu ID cannot be null")
  private Long menuId;

  @Schema(description = "Menu name")
  @NotBlank(
      groups = {Create.class, Update.class},
      message = "Menu name cannot be blank")
  @Size(max = 30, message = "Menu name length cannot exceed 30")
  private String menuName;

  @Schema(description = "Menu type (1: Directory, 2: Menu, 3: Function)")
  @NotNull(
      groups = {Create.class, Update.class},
      message = "Menu type cannot be null")
  private Integer menuType;

  @Schema(description = "Parent menu ID (0 if no parent)")
  @NotNull(
      groups = {Create.class, Update.class},
      message = "Parent ID cannot be null")
  private Long parentId;

  @Schema(description = "Sort order")
  private Integer sort;

  @Schema(description = "Route path")
  private String path;

  @Schema(description = "Component path")
  private String component;

  @Schema(description = "Disabled flag")
  private Boolean disabledFlag;

  @Schema(description = "Web permission string")
  private String webPerms;

  @Schema(description = "API permission string")
  private String apiPerms;

  @Schema(description = "Menu icon")
  private String icon;

  /** Validation group for create operations */
  public interface Create {}

  /** Validation group for update operations */
  public interface Update {}
}
