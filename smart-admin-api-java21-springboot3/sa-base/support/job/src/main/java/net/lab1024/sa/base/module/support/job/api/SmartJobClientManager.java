package net.lab1024.sa.base.module.support.job.api;

import cn.hutool.core.util.IdUtil;
import com.baomidou.lock.LockInfo;
import com.google.common.collect.Lists;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobMsg;
import net.lab1024.sa.base.module.support.job.config.SmartJobAutoConfiguration;
import net.lab1024.sa.base.module.support.job.core.SmartJob;
import net.lab1024.sa.base.module.support.job.core.SmartJobExecutor;
import net.lab1024.sa.base.module.support.job.core.SmartJobLauncher;
import net.lab1024.sa.base.module.support.job.repository.SmartJobRepository;
import net.lab1024.sa.base.module.support.job.repository.domain.SmartJobEntity;
import net.lab1024.sa.foundation.redislock.LockService;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * smart job 执行端管理 分布式系统之间 用发布/订阅消息的形式 来管理多个job
 *
 * @author huke
 * @since 2024/6/22 20:31
 */
@ConditionalOnBean(SmartJobAutoConfiguration.class)
@Slf4j
@Service
public class SmartJobClientManager {

  private final SmartJobLauncher jobLauncher;

  private final SmartJobRepository jobRepository;

  private final List<SmartJob> jobInterfaceList;

  private static final String EXECUTE_LOCK = "smart-job-lock-msg-execute-";

  private static final String TOPIC = "smart-job-instance";

  /** 锁持有时间：20秒 */
  private static final long LOCK_EXPIRE_MS = 20000L;

  private final LockService lockService;

  private final RTopic topic;

  private final SmartJobMsgListener jobMsgListener;

  public SmartJobClientManager(
      SmartJobLauncher jobLauncher,
      SmartJobRepository jobRepository,
      List<SmartJob> jobInterfaceList,
      RedissonClient redissonClient,
      LockService lockService) {
    this.jobLauncher = jobLauncher;
    this.jobRepository = jobRepository;
    this.jobInterfaceList = (jobInterfaceList == null) ? List.of() : List.copyOf(jobInterfaceList);
    this.lockService = lockService;

    // 添加监听器
    this.topic = redissonClient.getTopic(TOPIC);
    this.jobMsgListener = new SmartJobMsgListener();
    topic.addListener(SmartJobMsg.class, jobMsgListener);
    log.info("==== SmartJob ==== client-manager init");
  }

  /** 发布消息 */
  public void publishToClient(SmartJobMsg msgDTO) {
    msgDTO.setMsgId(IdUtil.fastSimpleUUID());
    topic.publish(msgDTO);
  }

  /** 处理消息 */
  private class SmartJobMsgListener implements MessageListener<SmartJobMsg> {

    @Override
    public void onMessage(CharSequence channel, SmartJobMsg msg) {
      log.info("==== SmartJob ==== on-message :{}", msg);
      // 判断消息类型 业务简单就直接判断 复杂的话可以策略模式
      SmartJobMsg.MsgTypeEnum msgType = msg.getMsgType();
      // 更新任务
      if (SmartJobMsg.MsgTypeEnum.UPDATE_JOB == msgType) {
        updateJob(msg.getJobId());
      }
      // 执行任务
      if (SmartJobMsg.MsgTypeEnum.EXECUTE_JOB == msgType) {
        executeJob(msg);
      }
    }
  }

  /**
   * 获取任务执行类
   *
   * @param jobClass
   * @return
   */
  private Optional<SmartJob> queryJobImpl(String jobClass) {
    return jobInterfaceList.stream()
        .filter(e -> Objects.equals(e.getClassName(), jobClass))
        .findFirst();
  }

  /**
   * 更新任务
   *
   * @param jobId
   */
  private void updateJob(Integer jobId) {
    SmartJobEntity jobEntity = jobRepository.getJobDao().selectById(jobId);
    if (null == jobEntity) {
      return;
    }
    jobLauncher.startOrRefreshJob(Lists.newArrayList(jobEntity));
  }

  /**
   * 立即执行任务
   *
   * @param msg
   */
  private void executeJob(SmartJobMsg msg) {
    Integer jobId = msg.getJobId();
    SmartJobEntity jobEntity = jobRepository.getJobDao().selectById(jobId);
    if (null == jobEntity) {
      return;
    }
    // 获取定时任务实现类
    Optional<SmartJob> optional = this.queryJobImpl(jobEntity.getJobClass());
    if (!optional.isPresent()) {
      return;
    }

    // 获取执行锁 使用 Lock4j
    LockInfo lockInfo =
        lockService.tryLockNonBlocking(EXECUTE_LOCK + msg.getMsgId(), LOCK_EXPIRE_MS);
    if (lockInfo == null) {
      // 无法获取锁，说明其他实例已经在处理此消息
      return;
    }

    try {
      // 通过执行器 执行任务
      jobEntity.setParam(msg.getParam());
      SmartJobExecutor jobExecutor =
          new SmartJobExecutor(jobEntity, jobRepository, optional.get(), lockService);
      jobExecutor.execute(msg.getUpdateName());
    } finally {
      lockService.releaseLock(lockInfo);
    }
  }

  @PreDestroy
  public void destroy() {
    topic.removeListener(jobMsgListener);
  }
}
