/**
 * LocalStorage 常量和工具函數
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */

/**
 * LocalStorage Key 前綴
 */
const KEY_PREFIX = 'smart_admin_';

/**
 * LocalStorage Key 常量
 */
export const LocalStorageKey = {
  /** 用戶 Token */
  USER_TOKEN: `${KEY_PREFIX}user_token`,

  /** 用戶權限點 */
  USER_POINTS: `${KEY_PREFIX}user_points`,

  /** 用戶的 Tag 列表 */
  USER_TAG_NAV: `${KEY_PREFIX}user_tag_nav`,

  /** App Config 配置信息 */
  APP_CONFIG: `${KEY_PREFIX}app_config`,

  /** 首頁快捷入口 */
  HOME_QUICK_ENTRY: `${KEY_PREFIX}home_quick_entry`,

  /** 通知信息已讀 */
  NOTICE_READ: `${KEY_PREFIX}notice_read`,

  /** 待辦 */
  TO_BE_DONE: `${KEY_PREFIX}to_be_done`,

  /** 多租戶 ID */
  TENANT_ID: `${KEY_PREFIX}tenant_id`,

  /** 多租戶代碼 */
  TENANT_CODE: `${KEY_PREFIX}tenant_code`,

  /** 多租戶時區 */
  TENANT_TIMEZONE: `${KEY_PREFIX}tenant_timezone`,
} as const;

/**
 * 讀取 LocalStorage
 */
export function localRead<T = any>(key: string): T | null {
  const value = localStorage.getItem(key);
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value) as T;
  } catch {
    return value as T;
  }
}

/**
 * 寫入 LocalStorage
 */
export function localSave(key: string, value: any): void {
  const data = typeof value === 'string' ? value : JSON.stringify(value);
  localStorage.setItem(key, data);
}

/**
 * 刪除 LocalStorage
 */
export function localRemove(key: string): void {
  localStorage.removeItem(key);
}

/**
 * 清空 LocalStorage
 */
export function localClear(): void {
  localStorage.clear();
}
