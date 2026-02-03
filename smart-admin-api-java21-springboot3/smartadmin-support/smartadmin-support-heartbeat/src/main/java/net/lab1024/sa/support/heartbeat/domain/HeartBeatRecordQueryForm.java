package net.lab1024.sa.support.heartbeat.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * 心跳记录 查询
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HeartBeatRecordQueryForm extends PageParam {

  @Schema(description = "关键字")
  private String keywords;

  @Schema(description = "开始日期")
  private LocalDate startDate;

  @Schema(description = "结束日期")
  private LocalDate endDate;
}
