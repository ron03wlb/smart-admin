/*
 * MFA Recovery API
 *
 * @Author:    SmartAdmin MFA Team
 * @Date:      2026-03-04
 * @Copyright  SmartAdmin
 */

import { getRequest, postRequest } from '/@/lib/axios';

export const mfaRecoveryApi = {
  /**
   * 創建 MFA 恢復請求
   * @param params { email: string, reason: string }
   */
  createRequest: (params: { email: string; reason: string }) => {
    return postRequest('/mfa/recovery/request', params);
  },

  /**
   * 查詢待審核恢復請求（Super Admin）
   */
  getPendingRequests: () => {
    return getRequest('/mfa/recovery/pending');
  },

  /**
   * 批准恢復請求（Super Admin）
   * @param recoveryId 恢復請求 ID
   */
  approveRequest: (recoveryId: number) => {
    return postRequest(`/mfa/recovery/approve?recoveryId=${recoveryId}`);
  },

  /**
   * 拒絕恢復請求（Super Admin）
   * @param params { recoveryId: number, rejectionReason: string }
   */
  rejectRequest: (params: { recoveryId: number; rejectionReason: string }) => {
    return postRequest('/mfa/recovery/reject', params);
  },

  /**
   * 使用恢復碼重置 MFA
   * @param params { recoveryCode: string }
   */
  resetWithCode: (params: { recoveryCode: string }) => {
    return postRequest('/mfa/recovery/reset', params);
  },
};
