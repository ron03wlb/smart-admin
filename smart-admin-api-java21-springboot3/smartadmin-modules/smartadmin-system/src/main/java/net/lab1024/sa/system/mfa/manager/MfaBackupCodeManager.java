package net.lab1024.sa.system.mfa.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import net.lab1024.sa.system.mfa.dao.MfaBackupCodeDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaBackupCodeEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MFA Backup Code Manager
 *
 * <p>Manager for MFA backup codes with transactional support. Each employee has 10 backup codes
 * (8-digit numbers), each usable only once.
 *
 * <p>Security:
 *
 * <ul>
 *   <li>Backup codes are hashed using Argon2id before storage (similar to password hashing)
 *   <li>Each code can only be used once (marked as used after verification)
 *   <li>Backup codes are regenerated when count drops below 2
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaBackupCodeManager {

  private static final int BACKUP_CODE_COUNT = 10;
  private static final int BACKUP_CODE_LENGTH = 8;
  private static final int LOW_CODE_THRESHOLD = 2;

  private final MfaBackupCodeDao mfaBackupCodeDao;
  private final PasswordEncryptService passwordEncryptService;

  /**
   * Generate backup codes for the given employee (transaction method).
   *
   * <p>Returns 10 plaintext backup codes (8-digit numbers). Codes are hashed before storage.
   *
   * @param employeeId Employee ID
   * @return List of 10 plaintext backup codes (8-digit strings)
   */
  @Transactional(rollbackFor = Throwable.class)
  public Try<List<String>> generateBackupCodes(Long employeeId) {
    return Try.of(
        () -> {
          // Soft delete all existing backup codes
          LambdaUpdateWrapper<MfaBackupCodeEntity> updateWrapper =
              new LambdaUpdateWrapper<MfaBackupCodeEntity>()
                  .eq(MfaBackupCodeEntity::getEmployeeId, employeeId)
                  .eq(MfaBackupCodeEntity::getDeleted, false)
                  .set(MfaBackupCodeEntity::getDeleted, true);
          mfaBackupCodeDao.update(null, updateWrapper);

          // Generate new backup codes
          List<String> plaintextCodes = new ArrayList<>();
          SecureRandom random = new SecureRandom();

          for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            // Generate 8-digit random number
            int code = 10000000 + random.nextInt(90000000);
            String plaintextCode = String.valueOf(code);
            plaintextCodes.add(plaintextCode);

            // Hash and store
            String hashedCode = passwordEncryptService.encrypt(plaintextCode);
            MfaBackupCodeEntity entity = new MfaBackupCodeEntity();
            entity.setEmployeeId(employeeId);
            entity.setCodeHash(hashedCode);
            entity.setUsed(false);
            entity.setDeleted(false);
            mfaBackupCodeDao.insert(entity);
          }

          log.info("Generated {} backup codes for employee ID: {}", BACKUP_CODE_COUNT, employeeId);
          return plaintextCodes;
        });
  }

  /**
   * Verify backup code and mark as used (transaction method).
   *
   * @param employeeId Employee ID
   * @param plaintextCode Plaintext backup code entered by user
   * @param ipAddress IP address of the request
   * @return true if code is valid and unused, false otherwise
   */
  @Transactional(rollbackFor = Throwable.class)
  public Try<Boolean> verifyBackupCode(Long employeeId, String plaintextCode, String ipAddress) {
    return Try.of(
        () -> {
          if (plaintextCode == null || plaintextCode.length() != BACKUP_CODE_LENGTH) {
            log.warn("Invalid backup code length: {}", plaintextCode);
            return false;
          }

          // Retrieve all unused backup codes for the employee
          List<MfaBackupCodeEntity> unusedCodes =
              mfaBackupCodeDao.selectList(
                  new LambdaQueryWrapper<MfaBackupCodeEntity>()
                      .eq(MfaBackupCodeEntity::getEmployeeId, employeeId)
                      .eq(MfaBackupCodeEntity::getUsed, false)
                      .eq(MfaBackupCodeEntity::getDeleted, false));

          if (unusedCodes.isEmpty()) {
            log.warn("No unused backup codes found for employee ID: {}", employeeId);
            return false;
          }

          // Verify code against each unused code (constant-time comparison)
          for (MfaBackupCodeEntity entity : unusedCodes) {
            if (passwordEncryptService.matches(plaintextCode, entity.getCodeHash())) {
              // Mark as used
              entity.setUsed(true);
              entity.setUsedAt(OffsetDateTime.now(ZoneOffset.UTC));
              entity.setUsedIp(ipAddress);
              mfaBackupCodeDao.updateById(entity);

              log.info(
                  "Backup code verified successfully for employee ID: {} from IP: {}",
                  employeeId,
                  ipAddress);

              // Check if remaining codes are below threshold
              int remainingCount = getRemainingCount(employeeId).getOrElse(0);
              if (remainingCount <= LOW_CODE_THRESHOLD) {
                log.warn(
                    "Low backup code count ({}) for employee ID: {}. User should regenerate codes.",
                    remainingCount,
                    employeeId);
              }

              return true;
            }
          }

          log.warn("Backup code verification failed for employee ID: {}", employeeId);
          return false;
        });
  }

  /**
   * Get remaining (unused) backup code count.
   *
   * @param employeeId Employee ID
   * @return Number of unused backup codes
   */
  public Option<Integer> getRemainingCount(Long employeeId) {
    return Option.of(
        mfaBackupCodeDao
            .selectCount(
                new LambdaQueryWrapper<MfaBackupCodeEntity>()
                    .eq(MfaBackupCodeEntity::getEmployeeId, employeeId)
                    .eq(MfaBackupCodeEntity::getUsed, false)
                    .eq(MfaBackupCodeEntity::getDeleted, false))
            .intValue());
  }

  /**
   * Check if backup codes need regeneration (count <= 2).
   *
   * @param employeeId Employee ID
   * @return true if regeneration needed, false otherwise
   */
  public boolean needsRegeneration(Long employeeId) {
    int remainingCount = getRemainingCount(employeeId).getOrElse(0);
    return remainingCount <= LOW_CODE_THRESHOLD;
  }
}
