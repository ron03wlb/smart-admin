package net.lab1024.sa.support.heartbeat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.support.heartbeat.core.HeartBeatRecord;
import net.lab1024.sa.support.heartbeat.core.IHeartBeatRecordHandler;
import net.lab1024.sa.support.heartbeat.dao.HeartBeatRecordDao;
import net.lab1024.sa.support.heartbeat.domain.HeartBeatRecordEntity;
import org.springframework.stereotype.Service;

/**
 * 心跳记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.LongVariable")
public class HeartBeatRecordHandler implements IHeartBeatRecordHandler {

  private final HeartBeatRecordDao heartBeatRecordDao;

  /**
   * 心跳日志处理方法
   *
   * @param heartBeatRecord
   */
  @Override
  public void handler(final HeartBeatRecord heartBeatRecord) {
    final HeartBeatRecordEntity heartBeatRecordEntity =
        SmartBeanUtil.copy(heartBeatRecord, HeartBeatRecordEntity.class);
    final HeartBeatRecordEntity heartBeatRecordOld =
        heartBeatRecordDao.query(heartBeatRecordEntity);
    if (heartBeatRecordOld == null) {
      heartBeatRecordDao.insert(heartBeatRecordEntity);
    } else {
      heartBeatRecordDao.updateHeartBeatTimeById(
          heartBeatRecordOld.getHeartBeatRecordId(), heartBeatRecordEntity.getHeartBeatTime());
    }
  }
}
