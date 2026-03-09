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
} as const;

export type LocalStorageKey = typeof LOCAL_STORAGE_KEYS[keyof typeof LOCAL_STORAGE_KEYS];
