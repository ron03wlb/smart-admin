/**
 * Reload Constants
 * 重載常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * Reload 權限點
 */
export const RELOAD_PERMISSION = {
  EXECUTE: 'support:reload:execute',
  RESULT: 'support:reload:result',
} as const;

/**
 * Reload 表格列寬度
 */
export const RELOAD_TABLE_COLUMNS_WIDTH = {
  tag: 200,
  identification: 0, // 自適應
  args: 0, // 自適應
  updateTime: 150,
  createTime: 150,
  action: 150,
} as const;
