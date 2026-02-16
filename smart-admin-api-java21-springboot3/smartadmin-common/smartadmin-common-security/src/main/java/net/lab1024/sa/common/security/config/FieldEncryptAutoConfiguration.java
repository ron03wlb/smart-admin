package net.lab1024.sa.common.security.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler;
import net.lab1024.sa.common.security.encrypt.FieldEncryptProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for AES-256-GCM field encryption and HMAC-SHA256 blind index
 *
 * <p>Activated when {@code smart.field-encrypt.enabled=true}. Registers encryption and blind index
 * beans, and wires the encryption service into the MyBatis TypeHandler.
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "smart.field-encrypt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FieldEncryptProperties.class)
public class FieldEncryptAutoConfiguration {

  private static final int AES_256_KEY_LENGTH = 32;

  @Bean
  @ConditionalOnMissingBean
  public AesGcmFieldEncryptService aesGcmFieldEncryptService(FieldEncryptProperties properties) {
    byte[] dekBytes = Base64.getDecoder().decode(properties.getDek());
    if (dekBytes.length != AES_256_KEY_LENGTH) {
      throw new IllegalArgumentException(
          "smart.field-encrypt.dek must be a 256-bit key (32 bytes), got " + dekBytes.length);
    }

    SecretKey secretKey = new SecretKeySpec(dekBytes, "AES");
    AesGcmFieldEncryptService service = new AesGcmFieldEncryptService(secretKey);

    // Wire into MyBatis TypeHandler
    EncryptedFieldTypeHandler.setEncryptService(service);

    log.info("AesGcmFieldEncryptService initialized (AES-256-GCM)");
    return service;
  }

  @Bean
  @ConditionalOnMissingBean
  public BlindIndexService blindIndexService(FieldEncryptProperties properties) {
    byte[] bikBytes = Base64.getDecoder().decode(properties.getBlindIndexKey());

    SecretKey secretKey = new SecretKeySpec(bikBytes, "HmacSHA256");
    BlindIndexService service = new BlindIndexService(secretKey);

    log.info("BlindIndexService initialized (HMAC-SHA256)");
    return service;
  }
}
