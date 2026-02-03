package net.lab1024.sa.support.job.repository;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.support.job.repository.domain.SmartJobEntity;
import net.lab1024.sa.support.job.repository.domain.SmartJobLogEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * job 持久化业务
 *
 * @author huke
 * @since 2024/6/22 22:28
 */
@Service
@RequiredArgsConstructor
public class SmartJobRepository {

  private final SmartJobDao jobDao;

  private final SmartJobLogDao jobLogDao;

  public SmartJobDao getJobDao() {
    return jobDao;
  }

  public SmartJobLogDao getJobLogDao() {
    return jobLogDao;
  }

  /**
   * 保存执行记录
   *
   * @param logEntity
   * @param jobEntity
   */
  @Transactional(rollbackFor = Throwable.class)
  public void saveLog(SmartJobLogEntity logEntity, SmartJobEntity jobEntity) {
    jobLogDao.insert(logEntity);

    jobEntity.setLastExecuteLogId(logEntity.getLogId());
    jobDao.updateById(jobEntity);
  }
}
