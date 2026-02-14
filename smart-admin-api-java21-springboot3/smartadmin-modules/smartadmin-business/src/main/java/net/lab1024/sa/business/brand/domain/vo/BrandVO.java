package net.lab1024.sa.business.brand.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Brand VO
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Data
@Schema(description = "Brand view object")
public class BrandVO {

  @Schema(description = "Brand ID (primary key)")
  private Long brandId;

  @Schema(description = "Brand name (unique)")
  private String brandName;

  @Schema(description = "Brand logo URL (image path)")
  private String brandLogo;

  @Schema(description = "Brand description")
  private String description;

  @Schema(description = "Display sort order (ascending)")
  private Integer sort;

  @Schema(description = "Status (1=Enabled, 0=Disabled)")
  private Integer status;

  @Schema(description = "Last update time")
  private LocalDateTime updateTime;

  @Schema(description = "Creation time")
  private LocalDateTime createTime;
}
