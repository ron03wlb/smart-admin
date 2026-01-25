package net.lab1024.sa.admin.module.business.brand.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * Brand Add Form
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Data
@Schema(description = "Brand add form")
public class BrandAddForm {

  @Schema(description = "Brand name (max 50 chars, unique)")
  @NotBlank(message = "Brand name cannot be empty")
  @Length(max = 50, message = "Brand name cannot exceed 50 characters")
  private String brandName;

  @Schema(description = "Brand logo URL (optional, max 200 chars)")
  @Length(max = 200, message = "Logo URL cannot exceed 200 characters")
  private String brandLogo;

  @Schema(description = "Description (optional, max 500 chars)")
  @Length(max = 500, message = "Description cannot exceed 500 characters")
  private String description;

  @Schema(description = "Display sort order (for UI ordering)")
  @NotNull(message = "Sort order cannot be empty")
  private Integer sort;

  @Schema(description = "Status (1=Enabled, 0=Disabled)")
  @NotNull(message = "Status cannot be empty")
  private Integer status;
}
