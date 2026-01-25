package net.lab1024.sa.admin.module.business.brand.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;

/**
 * Brand Query Form
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Brand query form")
public class BrandQueryForm extends PageParam {

  @Schema(description = "Search keyword (brand name)")
  private String keyword;

  @Schema(description = "Status filter (1=Enabled, 0=Disabled)")
  private Integer status;

  @Schema(description = "Deleted flag filter (true=deleted, false=active, null=all)")
  private Boolean deletedFlag;
}
