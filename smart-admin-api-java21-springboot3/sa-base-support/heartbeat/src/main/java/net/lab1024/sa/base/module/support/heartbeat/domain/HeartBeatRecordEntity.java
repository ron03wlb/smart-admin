package net.lab1024.sa.base.module.support.heartbeat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 心跳记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_heart_beat_record")
public class HeartBeatRecordEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键id */
  @TableId(type = IdType.AUTO)
  private Long heartBeatRecordId;

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
