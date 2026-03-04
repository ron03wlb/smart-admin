/*
 * MFA 多因素認證 API
 *
 * @Author:    SmartAdmin MFA Team
 * @Date:      2026-03-03
 * @Copyright  SmartAdmin
 */

import { getRequest, postRequest } from '/@/lib/axios';

export const mfaApi = {
  /**
   * 初始化 MFA 設定（生成 TOTP secret + QR 碼）
   */
  setupInit: () => {
    return postRequest('/mfa/setup/init');
  },

  /**
   * 啟用 MFA（驗證 TOTP + 生成備份碼）
   * @param params { totpToken: string, trustDevice?: boolean, deviceName?: string }
   */
  setupEnable: (params) => {
    return postRequest('/mfa/setup/enable', params);
  },

  /**
   * 禁用 MFA（需 TOTP 驗證）
   * @param totpToken TOTP 驗證碼（6位數字）
   */
  setupDisable: (totpToken) => {
    return postRequest(`/mfa/setup/disable?totpToken=${totpToken}`);
  },

  /**
   * 查詢 MFA 狀態
   */
  getStatus: () => {
    return getRequest('/mfa/status');
  },

  /**
   * 驗證 MFA（TOTP/備份碼）
   * @param params { mfaToken: string, trustDevice?: boolean, deviceName?: string }
   */
  verify: (params) => {
    return postRequest('/mfa/verify', params);
  },

  /**
   * 重新生成備份碼（需 TOTP 驗證）
   * @param totpToken TOTP 驗證碼（6位數字）
   */
  regenerateBackupCodes: (totpToken) => {
    return postRequest(`/mfa/backup-codes/regenerate?totpToken=${totpToken}`);
  },

  /**
   * 查詢剩餘備份碼數量
   */
  getBackupCodeCount: () => {
    return getRequest('/mfa/backup-codes/count');
  },

  /**
   * 添加信任設備
   * @param deviceName 設備名稱（例如：我的 iPhone 15）
   */
  addTrustedDevice: (deviceName) => {
    return postRequest(`/mfa/trusted-devices/add?deviceName=${encodeURIComponent(deviceName)}`);
  },
};
