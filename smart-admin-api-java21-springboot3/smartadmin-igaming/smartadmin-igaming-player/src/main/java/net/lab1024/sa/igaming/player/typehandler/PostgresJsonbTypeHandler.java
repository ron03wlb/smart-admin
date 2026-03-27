package net.lab1024.sa.igaming.player.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

/**
 * PostgreSQL JSONB TypeHandler for Map<String, Object>.
 *
 * <p>Converts between Java Map<String, Object> and PostgreSQL JSONB type.
 *
 * <p>Required for PostgreSQL JSONB columns when using MyBatis Plus.
 *
 * <p>Usage:
 *
 * <pre>
 * &#64;TableField(typeHandler = PostgresJsonbTypeHandler.class)
 * private Map<String, Object> metadata;
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-27
 */
@Slf4j
public class PostgresJsonbTypeHandler extends BaseTypeHandler<Map<String, Object>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
      throws SQLException {
    try {
      PGobject jsonObject = new PGobject();
      jsonObject.setType("jsonb");
      jsonObject.setValue(OBJECT_MAPPER.writeValueAsString(parameter));
      ps.setObject(i, jsonObject);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize Map to JSONB", e);
      throw new SQLException("Failed to serialize Map to JSONB", e);
    }
  }

  @Override
  public Map<String, Object> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return parseJson(rs.getString(columnName));
  }

  @Override
  public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return parseJson(rs.getString(columnIndex));
  }

  @Override
  public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parseJson(cs.getString(columnIndex));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJson(String json) throws SQLException {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JSONB to Map: {}", json, e);
      throw new SQLException("Failed to parse JSONB to Map", e);
    }
  }
}
