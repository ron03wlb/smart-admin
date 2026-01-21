package net.lab1024.sa.base.module.support.reload.config;

import net.lab1024.sa.base.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

/**
 * SmartAdmin Base Support Reload - AutoConfiguration for dynamic configuration reload module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class})
public class ReloadAutoConfiguration {
  // AutoConfiguration marker class
}
