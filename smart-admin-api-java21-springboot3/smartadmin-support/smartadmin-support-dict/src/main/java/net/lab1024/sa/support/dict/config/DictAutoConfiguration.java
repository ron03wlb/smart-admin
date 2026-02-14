package net.lab1024.sa.support.dict.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import net.lab1024.sa.common.mybatis.config.MybatisAutoConfiguration;
import net.lab1024.sa.common.web.web.config.WebAutoConfiguration;
import net.lab1024.sa.support.dict.json.deserializer.DictDataDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * SmartAdmin Base Support Dict - AutoConfiguration for dictionary module
 *
 * @author 1024创新实验室
 * @since 2026-01-21
 */
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class, MybatisAutoConfiguration.class})
public class DictAutoConfiguration {

  /** Register DictDataDeserializer with Jackson ObjectMapper via customizer pattern */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer dictJacksonCustomizer() {
    return builder -> {
      SimpleModule module = new SimpleModule("DictModule");
      module.addDeserializer(String.class, new DictDataDeserializer());
      builder.modules(module);
    };
  }
}
