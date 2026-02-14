package net.lab1024.sa.support.heartbeat.core;

import cn.hutool.core.net.NetUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 心跳线程
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class HeartBeatRunnable implements Runnable {

  /** 项目路径 */
  private String projectPath;

  /** 服务器ip（多网卡） */
  private List<String> serverIps;

  /** 进程号 */
  private Integer processNo;

  /** 进程开启时间 */
  private OffsetDateTime processStartTime;

  private IHeartBeatRecordHandler recordHandler;

  public HeartBeatRunnable(final IHeartBeatRecordHandler recordHandler) {
    this.recordHandler = recordHandler;
    this.initServerInfo();
  }

  /** 初始化心跳相关信息 */
  private void initServerInfo() {
    final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    this.projectPath = System.getProperty("user.dir");
    this.serverIps = new ArrayList<>(NetUtil.localIpv4s());

    // RuntimeMXBean.getName() 格式为 "pid@hostname"
    String[] nameParts = runtimeMXBean.getName().split("@");
    if (nameParts.length < 1) {
      log.warn("Unexpected runtime name format: {}", runtimeMXBean.getName());
      this.processNo = 0;
    } else {
      this.processNo = Integer.valueOf(nameParts[0]);
    }

    this.processStartTime =
        OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(runtimeMXBean.getStartTime()), ZoneOffset.UTC);
  }

  @Override
  public void run() {
    final HeartBeatRecord heartBeatRecord = new HeartBeatRecord();
    heartBeatRecord.setProjectPath(this.projectPath);
    heartBeatRecord.setServerIp(StringUtils.join(this.serverIps, ";"));
    heartBeatRecord.setProcessNo(this.processNo);
    heartBeatRecord.setProcessStartTime(this.processStartTime);
    heartBeatRecord.setHeartBeatTime(OffsetDateTime.now(ZoneOffset.UTC));
    recordHandler.handler(heartBeatRecord);
  }
}
