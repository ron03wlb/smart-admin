package net.lab1024.sa.base.module.support.file.config;

import net.lab1024.sa.base.mybatis.config.MybatisAutoConfiguration;
import net.lab1024.sa.base.web.config.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Import;

/**
 * SmartAdmin Base Support File - AutoConfiguration for file upload/download module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class, MybatisAutoConfiguration.class})
@Import({FileConfig.class})
public class FileAutoConfiguration {
  // AutoConfiguration marker class
  // Note: FileKey serializers/deserializers are registered via @Resource injection
  // and work through Spring's dependency injection, so no manual registration needed
}
