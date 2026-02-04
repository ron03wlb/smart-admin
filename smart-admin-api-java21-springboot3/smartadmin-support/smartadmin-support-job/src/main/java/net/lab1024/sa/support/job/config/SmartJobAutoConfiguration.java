package net.lab1024.sa.support.job.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import net.lab1024.sa.foundation.redislock.LockService;
import net.lab1024.sa.support.job.core.SmartJob;
import net.lab1024.sa.support.job.core.SmartJobLauncher;
import net.lab1024.sa.support.job.repository.SmartJobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务 配置
 *
 * @author huke
 * @since 2024/6/17 21:30
 */
@Configuration
@EnableConfigurationProperties(SmartJobConfig.class)
@ConditionalOnProperty(
    prefix = SmartJobConfig.CONFIG_PREFIX,
    name = "enabled",
    havingValue = "true")
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SmartJobAutoConfiguration {

  private final SmartJobConfig jobConfig;

  private final SmartJobRepository jobRepository;

  private final List<SmartJob> jobInterfaceList;

  public SmartJobAutoConfiguration(
      SmartJobConfig jobConfig, SmartJobRepository jobRepository, List<SmartJob> jobInterfaceList) {
    this.jobConfig = jobConfig;
    this.jobRepository = jobRepository;
    this.jobInterfaceList = (jobInterfaceList == null) ? List.of() : List.copyOf(jobInterfaceList);
  }

  /**
   * 定时任务启动器
   *
   * @return
   */
  @Bean
  public SmartJobLauncher initJobLauncher(LockService lockService) {
    return new SmartJobLauncher(jobConfig, jobRepository, jobInterfaceList, lockService);
  }
}
