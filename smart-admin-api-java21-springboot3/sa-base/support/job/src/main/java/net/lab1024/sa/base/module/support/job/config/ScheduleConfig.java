package net.lab1024.sa.base.module.support.job.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;

/**
 * 定时任务调度 配置
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-05-30 21:22:12 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Configuration
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ScheduleConfig implements SchedulingConfigurer {

  private ScheduledTaskRegistrar taskRegistrar;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    this.taskRegistrar = taskRegistrar;
  }

  public String destroy() {
    if (taskRegistrar == null) {
      return "无 @Scheduled 定时任务需要关闭！";
    }
    List<Task> taskList = new ArrayList<>();
    taskList.addAll(taskRegistrar.getCronTaskList());
    taskList.addAll(taskRegistrar.getTriggerTaskList());
    taskList.addAll(taskRegistrar.getFixedDelayTaskList());
    taskList.addAll(taskRegistrar.getFixedRateTaskList());

    taskRegistrar.destroy();

    List<String> taskNameList = taskList.stream().map(Task::toString).collect(Collectors.toList());
    return "已关闭 @Scheduled定时任务：" + taskNameList.size() + "个！";
  }
}
