package net.lab1024.sa.support.liteflow.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * LiteFlow 監控概覽 VO
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow監控概覽VO")
public class LiteFlowMonitorOverviewVO {

  @Schema(description = "總流程數")
  private Integer totalChains;

  @Schema(description = "今日執行數")
  private Integer todayExecutions;

  @Schema(description = "今日成功率（%）")
  private Double todaySuccessRate;
}
