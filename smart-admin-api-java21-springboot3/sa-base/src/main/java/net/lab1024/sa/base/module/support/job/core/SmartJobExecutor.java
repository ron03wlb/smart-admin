package net.lab1024.sa.base.module.support.job.core;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.baomidou.lock.LockInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.common.util.SmartIpUtil;
import net.lab1024.sa.base.module.support.job.constant.SmartJobConst;
import net.lab1024.sa.base.module.support.job.constant.SmartJobUtil;
import net.lab1024.sa.base.module.support.job.repository.SmartJobRepository;
import net.lab1024.sa.base.module.support.job.repository.domain.SmartJobEntity;
import net.lab1024.sa.base.module.support.job.repository.domain.SmartJobLogEntity;
import net.lab1024.sa.common.redislock.LockService;
import org.springframework.util.StopWatch;

/**
 * 定时任务 执行器
 *
 * @author huke
 * @since 2024/6/17 21:30
 */
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SmartJobExecutor implements Runnable {

  private final SmartJobEntity jobEntity;

  private final SmartJobRepository jobRepository;

  private final SmartJob jobInterface;

  private final LockService lockService;

  private static final String EXECUTE_LOCK = "smart-job-lock-execute-";

  /** 锁持有时间：30秒 */
  private static final long LOCK_EXPIRE_MS = 30000L;

  public SmartJobExecutor(
      SmartJobEntity jobEntity,
      SmartJobRepository jobRepository,
      SmartJob jobInterface,
      LockService lockService) {
    this.jobEntity = jobEntity;
    this.jobRepository = jobRepository;
    this.jobInterface = jobInterface;
    this.lockService = lockService;
  }

  /** 系统线程执行 */
  @Override
  public void run() {
    // 获取当前任务执行锁 最多持有30s自动释放
    Integer jobId = jobEntity.getJobId();
    LockInfo lockInfo = lockService.tryLockNonBlocking(EXECUTE_LOCK + jobId, LOCK_EXPIRE_MS);
    if (lockInfo == null) {
      // 无法获取锁，说明其他实例正在执行此任务
      return;
    }
    try {
      // 查询上次执行时间 校验执行间隔
      SmartJobEntity dbJobEntity = jobRepository.getJobDao().selectById(jobId);
      if (null == dbJobEntity) {
        return;
      }
      LocalDateTime lastExecuteTime = dbJobEntity.getLastExecuteTime();
      if (null != lastExecuteTime) {
        LocalDateTime nextTime =
            SmartJobUtil.queryNextTimeFromLast(
                    jobEntity.getTriggerType(), jobEntity.getTriggerValue(), lastExecuteTime, 1)
                .get(0);
        if (LocalDateTime.now().isBefore(nextTime)) {
          return;
        }
      }
      // 执行任务
      SmartJobLogEntity logEntity = this.execute(SmartJobConst.SYSTEM_NAME);
      if (log.isInfoEnabled()) {
        log.info(
            "==== SmartJob ==== execute job->{}, time-millis->{}ms",
            jobEntity.getJobName(),
            logEntity.getExecuteTimeMillis());
      }
    } catch (Exception t) {
      if (log.isErrorEnabled()) {
        log.error("==== SmartJob ==== execute err:", t);
      }
    } finally {
      lockService.releaseLock(lockInfo);
    }
  }

  /**
   * 执行任务
   *
   * @param executorName
   */
  public SmartJobLogEntity execute(String executorName) {
    // 保存执行记录
    LocalDateTime startTime = LocalDateTime.now();
    Long logId = this.saveLogBeforeExecute(jobEntity, executorName, startTime);

    // 执行计时
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    // 执行任务
    Integer successFlag = 1;
    String executeResult;
    try {
      executeResult = jobInterface.run(jobEntity.getParam());
      stopWatch.stop();
    } catch (Exception t) {
      stopWatch.stop();
      successFlag = 0;
      // ps:异常信息不大于数据库字段长度限制
      executeResult = ExceptionUtil.stacktraceToString(t, 1800);
      if (log.isErrorEnabled()) {
        log.error("==== SmartJob ==== execute err:", t);
      }
    }

    // 更新执行记录
    SmartJobLogEntity logEntity = new SmartJobLogEntity();
    logEntity.setLogId(logId);
    logEntity.setSuccessFlag(successFlag);
    long totalTimeMillis = stopWatch.getTotalTimeMillis();
    logEntity.setExecuteTimeMillis(totalTimeMillis);
    logEntity.setExecuteEndTime(startTime.plus(totalTimeMillis, ChronoUnit.MILLIS));
    logEntity.setExecuteResult(executeResult);
    jobRepository.getJobLogDao().updateById(logEntity);
    return logEntity;
  }

  /**
   * 执行前 保存执行记录
   *
   * @param jobEntity
   * @param executorName
   * @param executeTime
   * @return 返回执行记录id
   */
  private Long saveLogBeforeExecute(
      SmartJobEntity jobEntity, String executorName, LocalDateTime executeTime) {
    Integer jobId = jobEntity.getJobId();
    // 保存执行记录
    SmartJobLogEntity logEntity = new SmartJobLogEntity();
    logEntity.setJobId(jobId);
    logEntity.setJobName(jobEntity.getJobName());
    logEntity.setParam(jobEntity.getParam());
    logEntity.setSuccessFlag(1);
    // 执行开始时间
    logEntity.setExecuteStartTime(executeTime);
    logEntity.setExecuteEndTime(executeTime);
    logEntity.setExecuteTimeMillis(0L);
    logEntity.setCreateName(executorName);
    logEntity.setIp(SmartIpUtil.getLocalFirstIp());
    logEntity.setProcessId(SmartJobUtil.getProcessId());
    logEntity.setProgramPath(SmartJobUtil.getProgramPath());

    // 更新最后执行时间
    SmartJobEntity updateJobEntity = new SmartJobEntity();
    updateJobEntity.setJobId(jobId);
    updateJobEntity.setLastExecuteTime(executeTime);
    jobRepository.saveLog(logEntity, updateJobEntity);
    return logEntity.getLogId();
  }

  /**
   * 查询 当前任务信息
   *
   * @return
   */
  public SmartJobEntity getJob() {
    return jobEntity;
  }
}
