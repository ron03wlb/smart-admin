/**
 * Login Fail Constants
 * 登錄失敗常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 登錄失敗權限點
 */
export const LOGIN_FAIL_PERMISSION = {
  QUERY: 'support:loginFail:query',
} as const;

/**
 * 鎖定狀態枚舉
 */
export const LOCK_FLAG_ENUM = {
  UNLOCKED: { value: 0, label: '未鎖定', color: 'success' },
  LOCKED: { value: 1, label: '已鎖定', color: 'error' },
} as const;

/**
 * 登錄失敗表格列寬度
 */
export const LOGIN_FAIL_TABLE_COLUMNS_WIDTH = {
  loginName: 150,
  userType: 100,
  loginFailCount: 120,
  lockFlag: 100,
  loginLockBeginTime: 180,
  createTime: 180,
  updateTime: 180,
} as const;
