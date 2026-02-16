package net.lab1024.sa.common.security.encrypt;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * MyBatis TypeHandler for transparent AES-256-GCM field encryption/decryption
 *
 * <p>Encrypts values on DB write, decrypts on DB read. Uses a static reference to {@link
 * AesGcmFieldEncryptService} set by {@link
 * net.lab1024.sa.common.security.config.FieldEncryptAutoConfiguration}.
 *
 * <p>Usage in Entity:
 *
 * <pre>{@code
 * @TableField(typeHandler = EncryptedFieldTypeHandler.class)
 * private String encryptedPhone;
 * }</pre>
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Slf4j
public class EncryptedFieldTypeHandler extends BaseTypeHandler<String> {

  private static AesGcmFieldEncryptService encryptService;

  /**
   * Set the encryption service instance (called by auto-configuration)
   *
   * @param service the encryption service
   */
  public static void setEncryptService(AesGcmFieldEncryptService service) {
    encryptService = service;
    log.info("EncryptedFieldTypeHandler initialized with AesGcmFieldEncryptService");
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
      throws SQLException {
    if (encryptService != null) {
      ps.setString(i, encryptService.encrypt(parameter));
    } else {
      log.warn("EncryptedFieldTypeHandler: encryptService not initialized, storing plaintext");
      ps.setString(i, parameter);
    }
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String value = rs.getString(columnName);
    return decryptIfNeeded(value);
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    return decryptIfNeeded(value);
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String value = cs.getString(columnIndex);
    return decryptIfNeeded(value);
  }

  private String decryptIfNeeded(String value) {
    if (value == null) {
      return null;
    }
    if (encryptService != null && encryptService.isEncrypted(value)) {
      return encryptService.decrypt(value);
    }
    return value;
  }
}
