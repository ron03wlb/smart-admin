/**
 * Login Log Constants
 * 登錄日誌常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 登錄日誌權限點
 */
export const LOGIN_LOG_PERMISSION = {
  QUERY: 'support:loginLog:query',
} as const;

/**
 * 登錄結果枚舉
 */
export const LOGIN_RESULT_ENUM = {
  LOGIN_SUCCESS: { value: 0, label: '登錄成功', color: 'success' },
  LOGIN_FAIL: { value: 1, label: '登錄失敗', color: 'error' },
  LOGIN_OUT: { value: 2, label: '退出登錄', color: 'processing' },
} as const;

/**
 * 登錄日誌表格列寬度
 */
export const LOGIN_LOG_TABLE_COLUMNS_WIDTH = {
  userId: 70,
  userName: 120,
  userType: 80,
  loginIp: 120,
  loginIpRegion: 150,
  userAgent: 200,
  loginResult: 100,
  remark: 150,
  createTime: 180,
} as const;
