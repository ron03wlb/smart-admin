/**
 * Level 3 Protect API
 * 三級等保 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/level3-protect-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/types/response';

/**
 * 三級等保配置表單
 */
export interface Level3ProtectConfig {
  twoFactorLoginEnabled: boolean; // 雙因子登錄
  loginFailMaxTimes: number; // 最大連續登錄失敗次數
  loginFailLockMinutes: number; // 連續登錄失敗鎖定分鐘
  loginActiveTimeoutMinutes: number; // 登錄後無操作自動退出分鐘
  passwordComplexityEnabled: boolean; // 密碼複雜度
  regularChangePasswordMonths: number; // 定期修改密碼時間間隔（月）
  regularChangePasswordNotAllowRepeatTimes: number; // 定期修改密碼不允許重複次數
  fileDetectFlag: boolean; // 文件安全檢測
  maxUploadFileSizeMb: number; // 上傳文件大小限制（MB）
}

/**
 * 三級等保 API
 */
export const level3ProtectApi = {
  /**
   * 查詢三級等保配置
   */
  getConfig: (): Promise<ResponseDTO<string>> => {
    return request.get('/support/protect/level3protect/getConfig');
  },

  /**
   * 更新三級等保配置
   */
  updateConfig: (config: Level3ProtectConfig): Promise<ResponseDTO<void>> => {
    return request.post('/support/protect/level3protect/updateConfig', config);
  },
};
