package net.lab1024.sa.system.mfa.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.system.mfa.domain.entity.MfaBackupCodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MFA Backup Code DAO
 *
 * <p>Provides database access methods for MFA backup code management.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Mapper
public interface MfaBackupCodeDao extends BaseMapper<MfaBackupCodeEntity> {

  /**
   * Select all backup codes by employee ID (non-deleted records only).
   *
   * @param employeeId Employee ID
   * @return List of backup code entities
   */
  List<MfaBackupCodeEntity> selectByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * Select unused backup codes by employee ID.
   *
   * @param employeeId Employee ID
   * @return List of unused backup code entities
   */
  List<MfaBackupCodeEntity> selectUnusedByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * Count unused backup codes by employee ID.
   *
   * @param employeeId Employee ID
   * @return Number of unused backup codes
   */
  Integer countUnused(@Param("employeeId") Long employeeId);

  /**
   * Mark all backup codes as deleted for the given employee ID (soft delete).
   *
   * @param employeeId Employee ID
   * @return Number of records updated
   */
  Integer deleteByEmployeeId(@Param("employeeId") Long employeeId);
}
