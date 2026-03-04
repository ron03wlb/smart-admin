package net.lab1024.sa.system.mfa.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.system.mfa.domain.entity.MfaConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MFA Configuration DAO
 *
 * <p>Provides database access methods for MFA configuration management.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Mapper
public interface MfaConfigDao extends BaseMapper<MfaConfigEntity> {

  /**
   * Select MFA configuration by employee ID (non-deleted records only).
   *
   * @param employeeId Employee ID
   * @return MFA configuration entity or null if not found
   */
  MfaConfigEntity selectByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * Select enabled MFA configuration by employee ID.
   *
   * @param employeeId Employee ID
   * @return MFA configuration entity if enabled, otherwise null
   */
  MfaConfigEntity selectEnabledByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * Check if employee has MFA enabled.
   *
   * @param employeeId Employee ID
   * @return true if MFA is enabled, false otherwise
   */
  Boolean isMfaEnabled(@Param("employeeId") Long employeeId);
}
