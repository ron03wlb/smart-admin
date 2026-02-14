package net.lab1024.sa.support.heartbeat.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 心跳记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class HeartBeatRecordVO {

  private Integer heartBeatRecordId;

  @Schema(description = "项目路径")
  private String projectPath;

  @Schema(description = "服务器ip")
  private String serverIp;

  @Schema(description = "进程号")
  private Integer processNo;

  @Schema(description = "进程开启时间")
  private OffsetDateTime processStartTime;

  @Schema(description = "心跳当前时间")
  private OffsetDateTime heartBeatTime;
}
