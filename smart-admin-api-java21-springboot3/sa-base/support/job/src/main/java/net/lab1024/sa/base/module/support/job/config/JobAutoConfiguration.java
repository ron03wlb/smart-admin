package net.lab1024.sa.base.module.support.job.config;

import net.lab1024.sa.base.mybatis.config.MybatisAutoConfiguration;
import net.lab1024.sa.base.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base Support Job - AutoConfiguration for scheduled job module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class, MybatisAutoConfiguration.class})
@Import({ScheduleConfig.class})
public class JobAutoConfiguration {
  // AutoConfiguration marker class
}
