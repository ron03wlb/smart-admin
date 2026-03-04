package net.lab1024.sa.system.mfa.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.system.mfa.domain.entity.MfaTrustedDeviceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MFA Trusted Device DAO
 *
 * <p>Provides database access methods for MFA trusted device management.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Mapper
public interface MfaTrustedDeviceDao extends BaseMapper<MfaTrustedDeviceEntity> {

  /**
   * Select all trusted devices by employee ID (non-deleted records only).
   *
   * @param employeeId Employee ID
   * @return List of trusted device entities
   */
  List<MfaTrustedDeviceEntity> selectByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * Select valid (not expired) trusted device by employee ID and device fingerprint.
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @return Trusted device entity if valid, otherwise null
   */
  MfaTrustedDeviceEntity selectValidByFingerprint(
      @Param("employeeId") Long employeeId, @Param("deviceFingerprint") String deviceFingerprint);

  /**
   * Check if device is trusted (valid and not expired).
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @return true if device is trusted, false otherwise
   */
  Boolean isTrustedDevice(
      @Param("employeeId") Long employeeId, @Param("deviceFingerprint") String deviceFingerprint);

  /**
   * Mark trusted device as deleted (soft delete).
   *
   * @param deviceId Device ID
   * @return Number of records updated
   */
  Integer deleteByDeviceId(@Param("deviceId") Long deviceId);
}
