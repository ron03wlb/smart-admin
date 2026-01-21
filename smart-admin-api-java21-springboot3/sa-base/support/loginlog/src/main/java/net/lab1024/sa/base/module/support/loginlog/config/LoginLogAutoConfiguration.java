package net.lab1024.sa.base.module.support.loginlog.config;

import net.lab1024.sa.base.mybatis.config.MybatisAutoConfiguration;
import net.lab1024.sa.base.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

/**
 * SmartAdmin Base Support LoginLog - AutoConfiguration for login log module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class, MybatisAutoConfiguration.class})
public class LoginLogAutoConfiguration {
  // AutoConfiguration marker class
}
