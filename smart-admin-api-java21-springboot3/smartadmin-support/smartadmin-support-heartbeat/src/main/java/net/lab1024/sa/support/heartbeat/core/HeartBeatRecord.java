package net.lab1024.sa.support.heartbeat.core;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 心跳记录日志
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class HeartBeatRecord {

  /** 项目名字 */
  private String projectPath;

  /** 服务器ip */
  private String serverIp;

  /** 进程号 */
  private Integer processNo;

  /** 进程开启时间 */
  private LocalDateTime processStartTime;

  /** 心跳当前时间 */
  private LocalDateTime heartBeatTime;
}
