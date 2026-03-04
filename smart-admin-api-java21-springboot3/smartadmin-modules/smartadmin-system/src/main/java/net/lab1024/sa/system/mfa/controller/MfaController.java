package net.lab1024.sa.system.mfa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.system.mfa.domain.form.MfaEnableForm;
import net.lab1024.sa.system.mfa.domain.form.MfaVerifyForm;
import net.lab1024.sa.system.mfa.domain.vo.MfaSetupInitVO;
import net.lab1024.sa.system.mfa.domain.vo.MfaStatusVO;
import net.lab1024.sa.system.mfa.service.MfaService;
import org.springframework.web.bind.annotation.*;

/**
 * MFA 多因素認證 Controller
 *
 * <p>提供 MFA 相關的 API 端點，包括：
 *
 * <ul>
 *   <li>MFA 設定（初始化、啟用、禁用）
 *   <li>MFA 驗證（TOTP、備份碼）
 *   <li>備份碼管理（重新生成、查詢剩餘數量）
 *   <li>信任設備管理
 *   <li>MFA 狀態查詢
 * </ul>
 *
 * <p><strong>安全提示</strong>：
 *
 * <ul>
 *   <li>所有 API 端點均需登入後才能訪問
 *   <li>敏感操作（禁用 MFA、重新生成備份碼）需額外 TOTP 驗證
 *   <li>備份碼僅在首次生成時顯示一次
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MFA)
public class MfaController {

  private final MfaService mfaService;

  /**
   * 初始化 MFA 設定
   *
   * <p>生成 TOTP secret 和 QR 碼，供用戶掃描綁定 Authenticator。
   *
   * <p><strong>返回內容</strong>：
   *
   * <ul>
   *   <li>TOTP secret（Base32編碼）
   *   <li>QR 碼 URL（otpauth:// 協議）
   *   <li>QR 碼 Data URL（Base64 圖片）
   * </ul>
   *
   * @return MfaSetupInitVO - MFA 設定初始化響應
   */
  @Operation(summary = "初始化 MFA 設定 @author SmartAdmin MFA Team")
  @PostMapping("/mfa/setup/init")
  public ResponseDTO<MfaSetupInitVO> initSetup() {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    return mfaService.initSetup(employeeId);
  }

  /**
   * 啟用 MFA
   *
   * <p>用戶掃描 QR 碼後，輸入 TOTP token 以驗證綁定成功，然後生成備份碼。
   *
   * <p><strong>步驟</strong>：
   *
   * <ol>
   *   <li>用戶調用 /mfa/setup/init 獲取 QR 碼
   *   <li>用戶掃描 QR 碼到 Google Authenticator
   *   <li>用戶輸入 6 位 TOTP token 調用此 API
   *   <li>系統驗證 TOTP token 正確後啟用 MFA
   *   <li>系統生成 10 個備份碼並返回（僅顯示一次）
   * </ol>
   *
   * @param form MfaEnableForm - 包含 TOTP token 和可選的信任設備設定
   * @return MfaSetupInitVO - 包含備份碼列表（僅顯示一次）
   */
  @Operation(summary = "啟用 MFA @author SmartAdmin MFA Team")
  @PostMapping("/mfa/setup/enable")
  public ResponseDTO<MfaSetupInitVO> enableMfa(@Valid @RequestBody MfaEnableForm form) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    form.setEmployeeId(employeeId);
    String ipAddress = SmartRequestUtil.getRequestUser().getIp();
    String userAgent = SmartRequestUtil.getRequestUser().getUserAgent();
    return mfaService.enableMfa(form, ipAddress, userAgent);
  }

  /**
   * 禁用 MFA
   *
   * <p><strong>安全限制</strong>：
   *
   * <ul>
   *   <li>需要輸入當前 TOTP token 進行驗證
   *   <li>由角色強制要求 MFA 的用戶無法禁用（enforcedByRole=true）
   *   <li>禁用後會記錄 WARNING 級別審計日誌
   * </ul>
   *
   * @param totpToken TOTP 驗證碼（6位數字）
   * @return ResponseDTO<String> - 成功/失敗信息
   */
  @Operation(summary = "禁用 MFA（需 TOTP 驗證）@author SmartAdmin MFA Team")
  @PostMapping("/mfa/setup/disable")
  public ResponseDTO<String> disableMfa(@RequestParam String totpToken) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    String ipAddress = SmartRequestUtil.getRequestUser().getIp();
    String userAgent = SmartRequestUtil.getRequestUser().getUserAgent();
    return mfaService.disableMfa(employeeId, totpToken, ipAddress, userAgent);
  }

  /**
   * 查詢 MFA 狀態
   *
   * <p>返回當前用戶的 MFA 配置狀態，包括：
   *
   * <ul>
   *   <li>是否已啟用 MFA
   *   <li>MFA 類型（TOTP、SMS、Email）
   *   <li>備份碼狀態（是否已生成、剩餘數量）
   *   <li>最後驗證時間
   *   <li>是否由角色強制要求 MFA
   * </ul>
   *
   * @return MfaStatusVO - MFA 狀態查詢響應
   */
  @Operation(summary = "查詢 MFA 狀態 @author SmartAdmin MFA Team")
  @GetMapping("/mfa/status")
  public ResponseDTO<MfaStatusVO> getStatus() {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    return mfaService.getStatus(employeeId);
  }

  /**
   * 驗證 MFA
   *
   * <p>用於登入流程或敏感操作前的 MFA 驗證。支持兩種 token 類型：
   *
   * <ul>
   *   <li>6位 TOTP token（來自 Google Authenticator）
   *   <li>8位備份碼（一次性使用）
   * </ul>
   *
   * @param form MfaVerifyForm - 包含 MFA token 和可選的信任設備設定
   * @return ResponseDTO<String> - 驗證成功/失敗信息
   */
  @Operation(summary = "驗證 MFA（TOTP/備份碼）@author SmartAdmin MFA Team")
  @PostMapping("/mfa/verify")
  public ResponseDTO<String> verifyMfa(@Valid @RequestBody MfaVerifyForm form) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    form.setEmployeeId(employeeId);
    String ipAddress = SmartRequestUtil.getRequestUser().getIp();
    String userAgent = SmartRequestUtil.getRequestUser().getUserAgent();
    return mfaService.verifyMfa(form, ipAddress, userAgent);
  }

  /**
   * 重新生成備份碼
   *
   * <p><strong>使用場景</strong>：
   *
   * <ul>
   *   <li>備份碼剩餘數量 ≤ 2 時建議重新生成
   *   <li>備份碼遺失或洩露時需要重新生成
   * </ul>
   *
   * <p><strong>安全限制</strong>：
   *
   * <ul>
   *   <li>需要輸入當前 TOTP token 進行驗證
   *   <li>舊備份碼會被標記為已刪除
   *   <li>新備份碼僅顯示一次
   * </ul>
   *
   * @param totpToken TOTP 驗證碼（6位數字）
   * @return MfaSetupInitVO - 包含新的備份碼列表
   */
  @Operation(summary = "重新生成備份碼（需 TOTP 驗證）@author SmartAdmin MFA Team")
  @PostMapping("/mfa/backup-codes/regenerate")
  public ResponseDTO<MfaSetupInitVO> regenerateBackupCodes(@RequestParam String totpToken) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    String ipAddress = SmartRequestUtil.getRequestUser().getIp();
    String userAgent = SmartRequestUtil.getRequestUser().getUserAgent();
    return mfaService.regenerateBackupCodes(employeeId, totpToken, ipAddress, userAgent);
  }

  /**
   * 查詢剩餘備份碼數量
   *
   * <p>返回當前用戶剩餘的可用備份碼數量。如果數量 ≤ 2，建議用戶重新生成備份碼。
   *
   * @return ResponseDTO<Integer> - 剩餘備份碼數量（0-10）
   */
  @Operation(summary = "查詢剩餘備份碼數量 @author SmartAdmin MFA Team")
  @GetMapping("/mfa/backup-codes/count")
  public ResponseDTO<Integer> getBackupCodeCount() {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    return mfaService.getBackupCodeCount(employeeId);
  }

  /**
   * 添加信任設備
   *
   * <p>將當前設備添加到信任設備清單，30 天內使用此設備登入時無需 MFA 驗證。
   *
   * <p><strong>設備識別</strong>：
   *
   * <ul>
   *   <li>設備指紋 = SHA256(IP + User-Agent + Device UUID)
   *   <li>信任期限：30 天
   *   <li>自動過期：30 天後需重新驗證 MFA
   * </ul>
   *
   * @param deviceName 設備名稱（例如：我的 iPhone 15）
   * @return ResponseDTO<String> - 成功/失敗信息
   */
  @Operation(summary = "添加信任設備 @author SmartAdmin MFA Team")
  @PostMapping("/mfa/trusted-devices/add")
  public ResponseDTO<String> addTrustedDevice(@RequestParam String deviceName) {
    Long employeeId = SmartRequestUtil.getRequestUserId();
    String ipAddress = SmartRequestUtil.getRequestUser().getIp();
    String userAgent = SmartRequestUtil.getRequestUser().getUserAgent();
    return mfaService.addTrustedDevice(employeeId, deviceName, ipAddress, userAgent);
  }
}
