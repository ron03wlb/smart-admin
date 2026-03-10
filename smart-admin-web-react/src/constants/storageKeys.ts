/**
 * localStorage 鍵名常量定義
 * SmartAdmin React 專案統一管理所有 localStorage 鍵名
 */

export const LOCAL_STORAGE_KEYS = {
  /** 用戶 Token */
  USER_TOKEN: 'USER_TOKEN',

  /** 用戶信息 */
  USER_INFO: 'USER_INFO',

  /** 語言設置 */
  LOCALE: 'LOCALE',

  /** 側邊欄收起狀態 */
  SIDEBAR_COLLAPSED: 'SIDEBAR_COLLAPSED',

  /** 主題模式 */
  THEME_MODE: 'THEME_MODE',

  /** 應用配置 */
  APP_CONFIG: 'APP_CONFIG',

  /** 租戶 ID */
  TENANT_ID: 'TENANT_ID',

  /** 租戶時區 */
  TENANT_TIMEZONE: 'TENANT_TIMEZONE',

  /** 租戶代碼 */
  TENANT_CODE: 'TENANT_CODE',
} as const;

// 向後兼容的別名
export const STORAGE_KEYS = LOCAL_STORAGE_KEYS;

export type LocalStorageKey = (typeof LOCAL_STORAGE_KEYS)[keyof typeof LOCAL_STORAGE_KEYS];
