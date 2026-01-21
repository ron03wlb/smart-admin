package net.lab1024.sa.common.apiencrypt.config;

import net.lab1024.sa.common.apiencrypt.advice.DecryptRequestAdvice;
import net.lab1024.sa.common.apiencrypt.advice.EncryptResponseAdvice;
import net.lab1024.sa.common.apiencrypt.service.ApiEncryptService;
import net.lab1024.sa.common.apiencrypt.service.ApiEncryptServiceSmImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * API 加密自動配置
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/21 11:41:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
public class ApiEncryptAutoConfiguration {

  /**
   * 預設使用 SM4 加密實現
   *
   * <p>如果應用程式已經定義了 ApiEncryptService Bean，則不會創建此 Bean
   *
   * @return ApiEncryptService
   */
  @Bean
  @ConditionalOnMissingBean(ApiEncryptService.class)
  public ApiEncryptService apiEncryptService() {
    return new ApiEncryptServiceSmImpl();
  }

  @Bean
  @ConditionalOnMissingBean(DecryptRequestAdvice.class)
  public DecryptRequestAdvice decryptRequestAdvice() {
    return new DecryptRequestAdvice();
  }

  @Bean
  @ConditionalOnMissingBean(EncryptResponseAdvice.class)
  public EncryptResponseAdvice encryptResponseAdvice() {
    return new EncryptResponseAdvice();
  }
}
