package net.lab1024.sa.system.mfa.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.system.mfa.domain.entity.MfaAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MFA Audit Log DAO
 *
 * <p>Provides database access methods for MFA audit log management.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Mapper
public interface MfaAuditLogDao extends BaseMapper<MfaAuditLogEntity> {

  /**
   * Select audit logs by employee ID with pagination.
   *
   * @param page Page object for pagination
   * @param employeeId Employee ID
   * @return Paginated list of audit log entities
   */
  List<MfaAuditLogEntity> selectByEmployeeId(
      Page<MfaAuditLogEntity> page, @Param("employeeId") Long employeeId);

  /**
   * Select audit logs by severity with pagination.
   *
   * @param page Page object for pagination
   * @param severity Severity level (INFO, WARNING, CRITICAL)
   * @return Paginated list of audit log entities
   */
  List<MfaAuditLogEntity> selectBySeverity(
      Page<MfaAuditLogEntity> page, @Param("severity") String severity);

  /**
   * Select recent audit logs by employee ID (last N days).
   *
   * @param employeeId Employee ID
   * @param days Number of days to look back
   * @return List of audit log entities
   */
  List<MfaAuditLogEntity> selectRecentByEmployeeId(
      @Param("employeeId") Long employeeId, @Param("days") Integer days);

  /**
   * Count failed MFA verification attempts in the last N minutes.
   *
   * @param employeeId Employee ID
   * @param minutes Number of minutes to look back
   * @return Number of failed attempts
   */
  Integer countRecentFailures(
      @Param("employeeId") Long employeeId, @Param("minutes") Integer minutes);
}
