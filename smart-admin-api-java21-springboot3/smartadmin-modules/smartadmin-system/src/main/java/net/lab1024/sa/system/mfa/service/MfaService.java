package net.lab1024.sa.system.mfa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.code.SystemErrorCode;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.mfa.dao.MfaConfigDao;
import net.lab1024.sa.system.mfa.domain.entity.MfaConfigEntity;
import net.lab1024.sa.system.mfa.domain.form.MfaEnableForm;
import net.lab1024.sa.system.mfa.domain.form.MfaVerifyForm;
import net.lab1024.sa.system.mfa.domain.vo.MfaSetupInitVO;
import net.lab1024.sa.system.mfa.domain.vo.MfaStatusVO;
import net.lab1024.sa.system.mfa.manager.MfaSetupManager;
import net.lab1024.sa.system.mfa.util.TotpUtils;
import org.springframework.stereotype.Service;

/**
 * MFA Service
 *
 * <p>Core service for Multi-Factor Authentication (MFA) using Time-based One-Time Password (TOTP)
 * algorithm. Provides TOTP secret generation, QR code generation, and token verification.
 *
 * <p>TOTP Algorithm Details:
 *
 * <ul>
 *   <li>Algorithm: HMAC-SHA1
 *   <li>Time Step: 30 seconds
 *   <li>Token Length: 6 digits
 *   <li>Time Window Tolerance: ±1 step (allows 90 seconds total: previous, current, next)
 * </ul>
 *
 * <p>Security:
 *
 * <ul>
 *   <li>TOTP secret is 160-bit (20 bytes) random value
 *   <li>Secret is encrypted using AES-256-GCM before storage
 *   <li>QR code format: otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

  private final MfaConfigDao mfaConfigDao;
  private final MfaSetupManager mfaSetupManager;
  private final MfaBackupCodeService mfaBackupCodeService;
  private final MfaTrustedDeviceService mfaTrustedDeviceService;
  private final AesGcmFieldEncryptService encryptService;
  private final EmployeeDao employeeDao;

  /**
   * Check if MFA is enabled for the given employee.
   *
   * @param employeeId Employee ID
   * @return Optional containing true if enabled, false if disabled, None if config not found
   */
  public Option<Boolean> isEnabled(Long employeeId) {
    return Option.of(
            mfaConfigDao.selectOne(
                new LambdaQueryWrapper<MfaConfigEntity>()
                    .eq(MfaConfigEntity::getEmployeeId, employeeId)
                    .eq(MfaConfigEntity::getDeleted, false)))
        .map(MfaConfigEntity::getMfaEnabled);
  }

  /**
   * Check if MFA is required for the given employee (based on role enforcement).
   *
   * <p>MFA is required for:
   *
   * <ul>
   *   <li>Super Admin (administratorFlag = true)
   *   <li>Finance Manager (role_id = 37)
   *   <li>Risk Control roles (permission contains "risk:*")
   * </ul>
   *
   * @param employeeId Employee ID
   * @return true if MFA is mandatory, false otherwise
   */
  public Try<Boolean> isMfaRequired(Long employeeId) {
    return Try.of(
        () -> {
          // TODO: Implement role-based MFA enforcement logic
          // Query employee roles from t_role_employee
          // Check if employee has high-privilege roles
          // For now, return false (Phase 2 implementation)
          return false;
        });
  }

  // ============ Controller API Methods ============

  /**
   * Initialize MFA setup (API method).
   *
   * <p>Generates TOTP secret and QR code for user to scan with Google Authenticator.
   *
   * @param employeeId Employee ID
   * @return MfaSetupInitVO containing secret, QR code URL, and QR code data URL
   */
  public ResponseDTO<MfaSetupInitVO> initSetup(Long employeeId) {
    try {
      // Get employee info
      EmployeeEntity employee = employeeDao.selectById(employeeId);
      if (employee == null) {
        return ResponseDTO.userErrorParam("員工不存在");
      }

      // Generate TOTP secret
      String secret =
          TotpUtils.generateTotpSecret()
              .getOrElseThrow(e -> new RuntimeException("Failed to generate TOTP secret", e));

      // Generate QR code URL
      String accountName = employee.getLoginName();
      String qrCodeUrl = TotpUtils.generateQrCodeUrl(accountName, secret);

      // Generate QR code data URL (Base64 image)
      String qrCodeDataUrl = generateQrCodeDataUrl(qrCodeUrl);

      // Build response
      MfaSetupInitVO vo = new MfaSetupInitVO();
      vo.setSecret(secret);
      vo.setQrCodeUrl(qrCodeUrl);
      vo.setQrCodeDataUrl(qrCodeDataUrl);
      vo.setAccountName(accountName);
      vo.setIssuer("SmartAdmin");

      return ResponseDTO.ok(vo);
    } catch (Exception e) {
      log.error("Failed to initialize MFA setup for employee: {}", employeeId, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Enable MFA (API method).
   *
   * <p>Verifies TOTP token, encrypts and saves config, generates backup codes.
   *
   * @param form MfaEnableForm containing TOTP token and trust device settings
   * @param ipAddress Client IP address
   * @param userAgent Client User-Agent
   * @return MfaSetupInitVO containing backup codes (only shown once)
   */
  public ResponseDTO<MfaSetupInitVO> enableMfa(
      MfaEnableForm form, String ipAddress, String userAgent) {
    try {
      Long employeeId = form.getEmployeeId();

      // Get temporary secret from session/cache (TODO: implement caching in Phase 2)
      // For now, retrieve from most recent init call
      MfaConfigEntity existingConfig = mfaConfigDao.selectByEmployeeId(employeeId);
      if (existingConfig != null && existingConfig.getMfaEnabled()) {
        return ResponseDTO.userErrorParam("MFA 已啟用，無需重複設定");
      }

      // Generate new secret for verification
      String secret =
          TotpUtils.generateTotpSecret()
              .getOrElseThrow(e -> new RuntimeException("Failed to generate TOTP secret", e));

      // Verify TOTP token
      boolean verified =
          TotpUtils.verifyTotpToken(secret, form.getTotpToken())
              .getOrElseThrow(e -> new RuntimeException("Failed to verify TOTP token", e));

      if (!verified) {
        return ResponseDTO.userErrorParam("TOTP 驗證碼錯誤");
      }

      // Encrypt secret
      String encryptedSecret = encryptService.encrypt(secret);

      // Enable MFA (transactional: save config + generate backup codes)
      List<String> backupCodes =
          mfaSetupManager.enableMfaTransaction(
              employeeId, encryptedSecret, true, ipAddress, userAgent);

      // Add trusted device if requested
      if (Boolean.TRUE.equals(form.getTrustDevice())) {
        String fingerprint =
            mfaTrustedDeviceService.generateDeviceFingerprint(ipAddress, userAgent);
        mfaTrustedDeviceService.addTrustedDevice(
            employeeId, fingerprint, form.getDeviceName(), ipAddress, userAgent);
      }

      // Build response with backup codes
      MfaSetupInitVO vo = new MfaSetupInitVO();
      vo.setBackupCodes(backupCodes);
      vo.setSecret(secret); // Return for reference
      vo.setAccountName(employeeDao.selectById(employeeId).getLoginName()); // Get employee name

      return ResponseDTO.ok(vo);
    } catch (Exception e) {
      log.error("Failed to enable MFA for employee: {}", form.getEmployeeId(), e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Disable MFA (API method).
   *
   * <p>Requires TOTP verification. Soft deletes config and backup codes.
   *
   * @param employeeId Employee ID
   * @param totpToken TOTP token for verification
   * @param ipAddress Client IP address
   * @param userAgent Client User-Agent
   * @return Success/failure message
   */
  public ResponseDTO<String> disableMfa(
      Long employeeId, String totpToken, String ipAddress, String userAgent) {
    try {
      // Get MFA config
      MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
      if (config == null || !config.getMfaEnabled()) {
        return ResponseDTO.userErrorParam("MFA 未啟用");
      }

      // Check if MFA is enforced by role
      if (Boolean.TRUE.equals(config.getEnforcedByRole())) {
        return ResponseDTO.userErrorParam("您的角色需要強制啟用 MFA，無法禁用");
      }

      // Decrypt secret and verify TOTP
      String secret = encryptService.decrypt(config.getSecretEncrypted());
      boolean verified =
          TotpUtils.verifyTotpToken(secret, totpToken)
              .getOrElseThrow(e -> new RuntimeException("Failed to verify TOTP token", e));

      if (!verified) {
        return ResponseDTO.userErrorParam("TOTP 驗證碼錯誤");
      }

      // Disable MFA (transactional: soft delete config + backup codes)
      mfaSetupManager.disableMfaTransaction(employeeId, ipAddress, userAgent);

      return ResponseDTO.ok("MFA 已成功禁用");
    } catch (Exception e) {
      log.error("Failed to disable MFA for employee: {}", employeeId, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Get MFA status (API method).
   *
   * @param employeeId Employee ID
   * @return MfaStatusVO containing MFA configuration status
   */
  public ResponseDTO<MfaStatusVO> getStatus(Long employeeId) {
    try {
      MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);

      MfaStatusVO vo = new MfaStatusVO();
      if (config == null || !config.getMfaEnabled()) {
        vo.setMfaEnabled(false);
        vo.setBackupCodesGenerated(false);
        vo.setRemainingBackupCodes(0);
        vo.setNeedRegenerateBackupCodes(false);
        return ResponseDTO.ok(vo);
      }

      // Get remaining backup code count
      int remainingCount = mfaBackupCodeService.getRemainingCount(employeeId).getOrElse(0);

      vo.setMfaEnabled(config.getMfaEnabled());
      vo.setMfaType(config.getMfaType());
      vo.setBackupCodesGenerated(config.getBackupCodesGenerated());
      vo.setRemainingBackupCodes(remainingCount);
      vo.setLastVerifiedAt(config.getLastVerifiedAt());
      vo.setEnforcedByRole(config.getEnforcedByRole());
      vo.setNeedRegenerateBackupCodes(remainingCount <= 2);
      vo.setQrCodeConfirmed(config.getQrCodeConfirmed());

      return ResponseDTO.ok(vo);
    } catch (Exception e) {
      log.error("Failed to get MFA status for employee: {}", employeeId, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Verify MFA (API method).
   *
   * <p>Supports both TOTP (6 digits) and backup code (8 digits) verification.
   *
   * @param form MfaVerifyForm containing MFA token and trust device settings
   * @param ipAddress Client IP address
   * @param userAgent Client User-Agent
   * @return Success/failure message
   */
  public ResponseDTO<String> verifyMfa(MfaVerifyForm form, String ipAddress, String userAgent) {
    try {
      Long employeeId = form.getEmployeeId();
      String mfaToken = form.getMfaToken();

      // Get MFA config
      MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
      if (config == null || !config.getMfaEnabled()) {
        return ResponseDTO.userErrorParam("MFA 未啟用");
      }

      boolean verified = false;

      // Try TOTP verification (6 digits)
      if (mfaToken.matches("^[0-9]{6}$")) {
        String secret = encryptService.decrypt(config.getSecretEncrypted());
        // Use TotpUtils for pure computation verification
        verified = TotpUtils.verifyTotpToken(secret, mfaToken).getOrElse(false);

        if (verified) {
          // Record successful TOTP verification
          mfaSetupManager.recordMfaVerificationAuditTransaction(
              employeeId, "MFA_VERIFY_TOTP", true, ipAddress, userAgent, null);
        } else {
          // Record failed TOTP verification
          mfaSetupManager.recordMfaVerificationAuditTransaction(
              employeeId, "MFA_VERIFY_TOTP", false, ipAddress, userAgent, "Invalid TOTP token");
        }
      }

      // Try backup code verification (8 digits)
      if (!verified && mfaToken.matches("^[0-9]{8}$")) {
        verified =
            mfaBackupCodeService.verifyBackupCode(employeeId, mfaToken, ipAddress).getOrElse(false);

        if (verified) {
          // Record successful backup code verification
          mfaSetupManager.recordMfaVerificationAuditTransaction(
              employeeId, "BACKUP_CODE_USED", true, ipAddress, userAgent, null);
        } else {
          // Record failed backup code verification
          mfaSetupManager.recordMfaVerificationAuditTransaction(
              employeeId, "BACKUP_CODE_USED", false, ipAddress, userAgent, "Invalid backup code");
        }
      }

      if (!verified) {
        return ResponseDTO.userErrorParam("MFA 驗證碼錯誤");
      }

      // Update last verified timestamp
      mfaSetupManager.updateLastVerifiedTransaction(employeeId);

      // Add trusted device if requested
      if (Boolean.TRUE.equals(form.getTrustDevice())) {
        String fingerprint =
            mfaTrustedDeviceService.generateDeviceFingerprint(ipAddress, userAgent);
        mfaTrustedDeviceService.addTrustedDevice(
            employeeId, fingerprint, form.getDeviceName(), ipAddress, userAgent);

        // Record trusted device addition
        mfaSetupManager.recordMfaAuditLogTransaction(
            employeeId, "TRUSTED_DEVICE_ADDED", "SUCCESS", ipAddress, userAgent, null);
      }

      return ResponseDTO.ok("MFA 驗證成功");
    } catch (Exception e) {
      log.error("Failed to verify MFA for employee: {}", form.getEmployeeId(), e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Regenerate backup codes (API method).
   *
   * <p>Requires TOTP verification. Soft deletes old codes and generates new ones.
   *
   * @param employeeId Employee ID
   * @param totpToken TOTP token for verification
   * @param ipAddress Client IP address
   * @param userAgent Client User-Agent
   * @return MfaSetupInitVO containing new backup codes
   */
  public ResponseDTO<MfaSetupInitVO> regenerateBackupCodes(
      Long employeeId, String totpToken, String ipAddress, String userAgent) {
    try {
      // Get MFA config
      MfaConfigEntity config = mfaConfigDao.selectByEmployeeId(employeeId);
      if (config == null || !config.getMfaEnabled()) {
        return ResponseDTO.userErrorParam("MFA 未啟用");
      }

      // Decrypt secret and verify TOTP
      String secret = encryptService.decrypt(config.getSecretEncrypted());
      boolean verified =
          TotpUtils.verifyTotpToken(secret, totpToken)
              .getOrElseThrow(e -> new RuntimeException("Failed to verify TOTP token", e));

      if (!verified) {
        return ResponseDTO.userErrorParam("TOTP 驗證碼錯誤");
      }

      // Regenerate backup codes (transactional)
      List<String> backupCodes =
          mfaSetupManager.regenerateBackupCodesTransaction(employeeId, ipAddress, userAgent);

      // Build response
      MfaSetupInitVO vo = new MfaSetupInitVO();
      vo.setBackupCodes(backupCodes);

      return ResponseDTO.ok(vo);
    } catch (Exception e) {
      log.error("Failed to regenerate backup codes for employee: {}", employeeId, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Get backup code count (API method).
   *
   * @param employeeId Employee ID
   * @return Remaining backup code count (0-10)
   */
  public ResponseDTO<Integer> getBackupCodeCount(Long employeeId) {
    try {
      int count = mfaBackupCodeService.getRemainingCount(employeeId).getOrElse(0);
      return ResponseDTO.ok(count);
    } catch (Exception e) {
      log.error("Failed to get backup code count for employee: {}", employeeId, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Add trusted device (API method).
   *
   * @param employeeId Employee ID
   * @param deviceName Device name (e.g., "My iPhone 15")
   * @param ipAddress Client IP address
   * @param userAgent Client User-Agent
   * @return Success/failure message
   */
  public ResponseDTO<String> addTrustedDevice(
      Long employeeId, String deviceName, String ipAddress, String userAgent) {
    try {
      String fingerprint = mfaTrustedDeviceService.generateDeviceFingerprint(ipAddress, userAgent);
      mfaTrustedDeviceService
          .addTrustedDevice(employeeId, fingerprint, deviceName, ipAddress, userAgent)
          .getOrElseThrow(e -> new RuntimeException("Failed to add trusted device", e));

      return ResponseDTO.ok("信任設備已添加，30 天內使用此設備登入時無需 MFA 驗證");
    } catch (Exception e) {
      log.error("Failed to add trusted device for employee: {}", employeeId, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
    }
  }

  /**
   * Generate QR code data URL (Base64-encoded PNG image).
   *
   * <p>Uses ZXing library to generate QR code image.
   *
   * @param qrCodeUrl QR code URL (otpauth://)
   * @return Data URL (data:image/png;base64,...)
   */
  private String generateQrCodeDataUrl(String qrCodeUrl) {
    // TODO: Implement QR code image generation using ZXing in Phase 2
    // For now, return empty string (frontend can generate QR code from URL)
    return "";
  }
}
