package net.lab1024.sa.common.security.encrypt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Field encryption configuration properties
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Data
@ConfigurationProperties(prefix = "smart.field-encrypt")
public class FieldEncryptProperties {

  /** Enable field encryption */
  private boolean enabled = false;

  /** Base64-encoded 256-bit AES Data Encryption Key */
  private String dek;

  /** Base64-encoded HMAC-SHA256 Blind Index Key */
  private String blindIndexKey;
}
