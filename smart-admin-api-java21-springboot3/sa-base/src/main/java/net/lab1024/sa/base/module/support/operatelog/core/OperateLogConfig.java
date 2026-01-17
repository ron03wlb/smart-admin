package net.lab1024.sa.base.module.support.operatelog.core;

import java.util.function.Function;
import lombok.Builder;
import lombok.Data;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogEntity;

/**
 * 配置
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021-12-08 20:48:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@Builder
public class OperateLogConfig {

  /** 操作日志存储方法 */
  private Function<OperateLogEntity, Boolean> saveFunction;

  /** 核心线程数 */
  private Integer corePoolSize;

  /** 队列大小 */
  private Integer queueCapacity;
}
