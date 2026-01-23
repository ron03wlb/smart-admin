package net.lab1024.sa.foundation.datamasking.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 数据脱敏自动配置
 *
 * <p>Data masking is annotation-driven and works automatically through Jackson's serialization
 * framework. No explicit bean registration is needed as the {@code @DataMasking} annotation
 * triggers the {@code DataMaskingSerializer} via Jackson's {@code @JsonSerialize}.
 *
 * @author 1024创新实验室
 * @since 2024/8/1
 */
@AutoConfiguration
public class DataMaskingAutoConfiguration {
  // Empty - annotation-based, no beans needed
  // Spring Boot auto-configures Jackson serializers via annotations
}
