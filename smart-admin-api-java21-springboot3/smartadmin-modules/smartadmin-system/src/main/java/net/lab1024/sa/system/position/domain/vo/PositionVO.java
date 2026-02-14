package net.lab1024.sa.system.position.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 职务表 列表VO
 *
 * @author kaiyun
 * @since 2024-06-23 23:31:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class PositionVO {

  @Schema(description = "职务ID")
  private Long positionId;

  @Schema(description = "职务名称")
  private String positionName;

  @Schema(description = "职级")
  private String positionLevel;

  @Schema(description = "排序")
  private Integer sort;

  @Schema(description = "备注")
  private String remark;

  @Schema(description = "创建时间")
  private OffsetDateTime createTime;

  @Schema(description = "更新时间")
  private OffsetDateTime updateTime;
}
