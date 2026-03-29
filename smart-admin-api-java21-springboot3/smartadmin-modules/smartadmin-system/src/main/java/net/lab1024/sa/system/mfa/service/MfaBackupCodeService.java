package net.lab1024.sa.system.mfa.service;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.mfa.manager.MfaBackupCodeManager;
import org.springframework.stereotype.Service;

/**
 * MFA Backup Code Service (Delegation Layer)
 *
 * <p>This service acts as a delegation layer for MFA backup code operations, forwarding all
 * requests to the underlying MfaBackupCodeManager. This design maintains backward compatibility
 * with existing code while ensuring proper layered architecture (Service → Manager → Dao).
 *
 * <p>Business logic, transaction management, and security features are implemented in
 * MfaBackupCodeManager.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaBackupCodeService {

  private final MfaBackupCodeManager mfaBackupCodeManager;

  /**
   * Generate backup codes for the given employee.
   *
   * <p>Delegates to MfaBackupCodeManager for implementation.
   *
   * @param employeeId Employee ID
   * @return List of 10 plaintext backup codes (8-digit strings)
   */
  public Try<List<String>> generateBackupCodes(Long employeeId) {
    return mfaBackupCodeManager.generateBackupCodesTransaction(employeeId);
  }

  /**
   * Verify backup code and mark as used.
   *
   * <p>Delegates to MfaBackupCodeManager for implementation.
   *
   * @param employeeId Employee ID
   * @param plaintextCode Plaintext backup code entered by user
   * @param ipAddress IP address of the request
   * @return true if code is valid and unused, false otherwise
   */
  public Try<Boolean> verifyBackupCode(Long employeeId, String plaintextCode, String ipAddress) {
    return mfaBackupCodeManager.verifyBackupCodeTransaction(employeeId, plaintextCode, ipAddress);
  }

  /**
   * Get remaining (unused) backup code count.
   *
   * <p>Delegates to MfaBackupCodeManager for implementation.
   *
   * @param employeeId Employee ID
   * @return Number of unused backup codes
   */
  public Option<Integer> getRemainingCount(Long employeeId) {
    return mfaBackupCodeManager.getRemainingCount(employeeId);
  }

  /**
   * Check if backup codes need regeneration (count <= 2).
   *
   * <p>Delegates to MfaBackupCodeManager for implementation.
   *
   * @param employeeId Employee ID
   * @return true if regeneration needed, false otherwise
   */
  public boolean needsRegeneration(Long employeeId) {
    return mfaBackupCodeManager.needsRegeneration(employeeId);
  }
}
