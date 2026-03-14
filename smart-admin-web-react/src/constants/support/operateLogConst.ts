/**
 * Operate Log Constants
 * 操作日誌常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * 操作日誌權限點
 */
export const OPERATE_LOG_PERMISSION = {
  QUERY: 'support:operateLog:query',
  DETAIL: 'support:operateLog:detail',
} as const;

/**
 * 請求結果狀態枚舉
 */
export const SUCCESS_FLAG_ENUM = {
  FAILURE: { value: 0, label: '失敗', color: 'error' },
  SUCCESS: { value: 1, label: '成功', color: 'success' },
} as const;

/**
 * 操作日誌表格列寬度
 */
export const OPERATE_LOG_TABLE_COLUMNS_WIDTH = {
  operateUserName: 70,
  operateUserType: 50,
  module: 120,
  content: 150,
  url: 200,
  response: 150,
  ipRegion: 150,
  userAgent: 150,
  createTime: 150,
  successFlag: 60,
  action: 60,
} as const;
