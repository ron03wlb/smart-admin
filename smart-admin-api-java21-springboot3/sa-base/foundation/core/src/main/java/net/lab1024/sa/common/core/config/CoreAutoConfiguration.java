package net.lab1024.sa.common.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Core 模块自动配置
 *
 * <p>提供基础的 domain、util、code、enumeration、exception、constant 类
 *
 * @author 1024创新实验室
 * @since 2024/01/20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
@ComponentScan(basePackages = "net.lab1024.sa.common.core")
public class CoreAutoConfiguration {
  // Core module only provides utility classes, no beans needed
}
