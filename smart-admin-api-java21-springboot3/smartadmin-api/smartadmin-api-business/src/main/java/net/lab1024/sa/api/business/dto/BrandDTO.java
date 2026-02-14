package net.lab1024.sa.api.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 品牌 DTO
 *
 * <p>用於品牌信息的跨模塊傳輸，支持 Feign 遠程調用。
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
@Schema(description = "品牌 DTO")
public class BrandDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "品牌 ID")
  private Long brandId;

  @Schema(description = "品牌名稱")
  private String brandName;

  @Schema(description = "品牌 Logo")
  private String logo;

  @Schema(description = "品牌描述")
  private String description;

  @Schema(description = "排序")
  private Integer sort;

  @Schema(description = "禁用標識")
  private Boolean disabledFlag;

  @Schema(description = "更新時間")
  private OffsetDateTime updateTime;

  @Schema(description = "創建時間")
  private OffsetDateTime createTime;
}
