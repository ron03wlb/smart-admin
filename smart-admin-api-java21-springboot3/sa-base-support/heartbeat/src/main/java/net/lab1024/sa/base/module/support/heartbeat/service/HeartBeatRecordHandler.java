package net.lab1024.sa.base.module.support.heartbeat.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.heartbeat.core.HeartBeatRecord;
import net.lab1024.sa.base.module.support.heartbeat.core.IHeartBeatRecordHandler;
import net.lab1024.sa.base.module.support.heartbeat.dao.HeartBeatRecordDao;
import net.lab1024.sa.base.module.support.heartbeat.domain.HeartBeatRecordEntity;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 心跳记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class HeartBeatRecordHandler implements IHeartBeatRecordHandler {

  @Resource private HeartBeatRecordDao heartBeatRecordDao;

  /**
   * 心跳日志处理方法
   *
   * @param heartBeatRecord
   */
  @Override
  public void handler(HeartBeatRecord heartBeatRecord) {
    HeartBeatRecordEntity heartBeatRecordEntity =
        SmartBeanUtil.copy(heartBeatRecord, HeartBeatRecordEntity.class);
    HeartBeatRecordEntity heartBeatRecordOld = heartBeatRecordDao.query(heartBeatRecordEntity);
    if (heartBeatRecordOld == null) {
      heartBeatRecordDao.insert(heartBeatRecordEntity);
    } else {
      heartBeatRecordDao.updateHeartBeatTimeById(
          heartBeatRecordOld.getHeartBeatRecordId(), heartBeatRecordEntity.getHeartBeatTime());
    }
  }
}
