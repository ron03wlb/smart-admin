package net.lab1024.sa.system.mfa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.mfa.dao.MfaTrustedDeviceDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaTrustedDeviceEntity;
import org.springframework.stereotype.Service;

/**
 * MFA Trusted Device Service
 *
 * <p>Service for managing trusted devices that can bypass MFA verification for 30 days. Device
 * fingerprint is generated from IP + User-Agent + Device UUID and hashed with SHA256.
 *
 * <p>Security:
 *
 * <ul>
 *   <li>Device fingerprint is one-way hashed (SHA256) to prevent reverse engineering
 *   <li>Trust period: 30 days from creation
 *   <li>Auto-expiry: Trusted devices are automatically invalidated after 30 days
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaTrustedDeviceService {

  private static final int TRUST_PERIOD_DAYS = 30;

  private final MfaTrustedDeviceDao mfaTrustedDeviceDao;

  /**
   * Generate device fingerprint from IP + User-Agent.
   *
   * <p>Uses SHA256 hash to generate a unique, one-way fingerprint.
   *
   * @param ipAddress IP address
   * @param userAgent User-Agent string
   * @return SHA256 fingerprint (64-character hex string)
   */
  public String generateDeviceFingerprint(String ipAddress, String userAgent) {
    try {
      String rawFingerprint = ipAddress + "|" + userAgent;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawFingerprint.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      log.error("Failed to generate device fingerprint", e);
      throw new RuntimeException("Failed to generate device fingerprint", e);
    }
  }

  /**
   * Check if device is trusted (valid and not expired).
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @return true if device is trusted, false otherwise
   */
  public Try<Boolean> isTrustedDevice(Long employeeId, String deviceFingerprint) {
    return Try.of(
        () -> {
          MfaTrustedDeviceEntity entity =
              mfaTrustedDeviceDao.selectOne(
                  new LambdaQueryWrapper<MfaTrustedDeviceEntity>()
                      .eq(MfaTrustedDeviceEntity::getEmployeeId, employeeId)
                      .eq(MfaTrustedDeviceEntity::getDeviceFingerprint, deviceFingerprint)
                      .eq(MfaTrustedDeviceEntity::getDeleted, false)
                      .gt(
                          MfaTrustedDeviceEntity::getTrustedUntil,
                          OffsetDateTime.now(ZoneOffset.UTC)));

          return entity != null;
        });
  }

  /**
   * Add a new trusted device.
   *
   * @param employeeId Employee ID
   * @param deviceFingerprint Device fingerprint (SHA256 hash)
   * @param deviceName Device name (user-provided, e.g., "My iPhone 15")
   * @param ipAddress IP address
   * @param userAgent User-Agent string
   * @return void
   */
  public Try<Void> addTrustedDevice(
      Long employeeId,
      String deviceFingerprint,
      String deviceName,
      String ipAddress,
      String userAgent) {
    return Try.run(
        () -> {
          // Check if device already exists (avoid duplicates)
          MfaTrustedDeviceEntity existing =
              mfaTrustedDeviceDao.selectOne(
                  new LambdaQueryWrapper<MfaTrustedDeviceEntity>()
                      .eq(MfaTrustedDeviceEntity::getEmployeeId, employeeId)
                      .eq(MfaTrustedDeviceEntity::getDeviceFingerprint, deviceFingerprint)
                      .eq(MfaTrustedDeviceEntity::getDeleted, false));

          if (existing != null) {
            // Update trust expiry (extend trust period)
            existing.setTrustedUntil(
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(TRUST_PERIOD_DAYS));
            mfaTrustedDeviceDao.updateById(existing);
            log.info(
                "Extended trust period for existing device (employee ID: {}, fingerprint: {})",
                employeeId,
                deviceFingerprint.substring(0, 8) + "...");
          } else {
            // Create new trusted device
            MfaTrustedDeviceEntity entity = new MfaTrustedDeviceEntity();
            entity.setEmployeeId(employeeId);
            entity.setDeviceFingerprint(deviceFingerprint);
            entity.setDeviceName(deviceName);
            entity.setIpAddress(ipAddress);
            entity.setUserAgent(userAgent);
            entity.setTrustedUntil(OffsetDateTime.now(ZoneOffset.UTC).plusDays(TRUST_PERIOD_DAYS));
            entity.setDeleted(false);
            mfaTrustedDeviceDao.insert(entity);
            log.info(
                "Added new trusted device (employee ID: {}, name: {}, fingerprint: {})",
                employeeId,
                deviceName,
                deviceFingerprint.substring(0, 8) + "...");
          }
        });
  }

  /**
   * Remove a trusted device (soft delete).
   *
   * @param deviceId Device ID
   * @return void
   */
  public Try<Void> removeTrustedDevice(Long deviceId) {
    return Try.run(
        () -> {
          MfaTrustedDeviceEntity entity = mfaTrustedDeviceDao.selectById(deviceId);
          if (entity != null) {
            entity.setDeleted(true);
            mfaTrustedDeviceDao.updateById(entity);
            log.info("Removed trusted device (device ID: {})", deviceId);
          }
        });
  }
}
