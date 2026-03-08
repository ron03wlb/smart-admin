/**
 * LocalStorage Key Constants
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */

/**
 * LocalStorage 鍵名常量
 */
export const LocalStorageKey = {
  /** 用戶 Token */
  USER_TOKEN: 'USER_TOKEN',

  /** 記住密碼 */
  REMEMBER_PWD: 'REMEMBER_PWD',

  /** 租戶 ID */
  TENANT_ID: 'TENANT_ID',

  /** 租戶時區 */
  TENANT_TIMEZONE: 'TENANT_TIMEZONE',

  /** 語言設置 */
  LANGUAGE: 'LANGUAGE',

  /** 主題設置 */
  THEME: 'THEME',
} as const;

export type LocalStorageKey = (typeof LocalStorageKey)[keyof typeof LocalStorageKey];
