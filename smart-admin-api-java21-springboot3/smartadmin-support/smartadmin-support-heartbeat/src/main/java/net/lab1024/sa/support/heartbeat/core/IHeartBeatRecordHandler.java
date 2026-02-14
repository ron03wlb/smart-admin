package net.lab1024.sa.support.heartbeat.core;

/**
 * 心跳处理接口
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface IHeartBeatRecordHandler {

  /**
   * 心跳日志处理方法
   *
   * @param heartBeatRecord
   */
  void handler(HeartBeatRecord heartBeatRecord);
}
