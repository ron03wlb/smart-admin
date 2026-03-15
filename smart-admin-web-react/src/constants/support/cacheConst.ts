/**
 * Cache Constants
 * 緩存常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 緩存權限點
 */
export const CACHE_PERMISSION = {
  DELETE: 'support:cache:delete',
  KEYS: 'support:cache:keys',
} as const;

/**
 * 緩存表格列寬度
 */
export const CACHE_TABLE_COLUMNS_WIDTH = {
  key: 0, // 自適應
  action: 160,
} as const;
