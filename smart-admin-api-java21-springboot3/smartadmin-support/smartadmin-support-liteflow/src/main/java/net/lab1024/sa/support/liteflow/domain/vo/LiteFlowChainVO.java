package net.lab1024.sa.support.liteflow.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * LiteFlow 流程 VO
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow流程VO")
public class LiteFlowChainVO {

  @Schema(description = "流程ID")
  private Long chainId;

  @Schema(description = "流程名稱")
  private String chainName;

  @Schema(description = "流程編碼")
  private String chainCode;

  @Schema(description = "流程類型")
  private Integer chainType;

  @Schema(description = "流程定義")
  private String chainData;

  @Schema(description = "版本號")
  private Integer version;

  @Schema(description = "狀態 0-禁用 1-啟用")
  private Integer status;

  @Schema(description = "備註")
  private String remark;

  @Schema(description = "創建時間")
  private LocalDateTime createTime;

  @Schema(description = "更新時間")
  private LocalDateTime updateTime;
}
