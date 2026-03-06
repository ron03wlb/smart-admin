package net.lab1024.sa.common.mybatis.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Boolean ↔ PostgreSQL SMALLINT (0/1) Type Converter
 *
 * <p>Purpose: - Java Boolean → DB SMALLINT (true=1, false=0, null=null) - DB SMALLINT → Java
 * Boolean (1=true, 0=false, null=null)
 *
 * <p>Use Cases: - EmployeeEntity.administratorFlag (SMALLINT) - EmployeeEntity.disabledFlag
 * (SMALLINT) - EmployeeEntity.deletedFlag (SMALLINT) - LoginFailEntity.lockFlag (SMALLINT) - Other
 * *Flag fields with DB type SMALLINT
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @TableField(typeHandler = BooleanToSmallintTypeHandler.class)
 * private Boolean administratorFlag;
 * }</pre>
 *
 * @author SmartAdmin Team
 * @since v4.1.0
 */
@MappedTypes(Boolean.class)
@MappedJdbcTypes(JdbcType.SMALLINT)
public class BooleanToSmallintTypeHandler extends BaseTypeHandler<Boolean> {

  /**
   * Set parameter (Java Boolean → DB SMALLINT)
   *
   * @param ps PreparedStatement
   * @param i Parameter position
   * @param parameter Boolean value (nullable)
   * @param jdbcType JDBC type
   * @throws SQLException if database access error occurs
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
      throws SQLException {
    // true → 1, false → 0
    ps.setInt(i, parameter ? 1 : 0);
  }

  /**
   * Get result by column name (DB SMALLINT → Java Boolean)
   *
   * @param rs ResultSet
   * @param columnName Column name
   * @return Boolean value (nullable)
   * @throws SQLException if database access error occurs
   */
  @Override
  public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
    int value = rs.getInt(columnName);
    if (rs.wasNull()) {
      return null; // DB NULL → Java null
    }
    return value == 1; // 1 → true, 0 → false
  }

  /**
   * Get result by column index (DB SMALLINT → Java Boolean)
   *
   * @param rs ResultSet
   * @param columnIndex Column index
   * @return Boolean value (nullable)
   * @throws SQLException if database access error occurs
   */
  @Override
  public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    int value = rs.getInt(columnIndex);
    if (rs.wasNull()) {
      return null;
    }
    return value == 1;
  }

  /**
   * Get result from CallableStatement (DB SMALLINT → Java Boolean)
   *
   * @param cs CallableStatement
   * @param columnIndex Column index
   * @return Boolean value (nullable)
   * @throws SQLException if database access error occurs
   */
  @Override
  public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    int value = cs.getInt(columnIndex);
    if (cs.wasNull()) {
      return null;
    }
    return value == 1;
  }
}
