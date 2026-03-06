/**
 * LocalStorage 工具函數
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */

/**
 * 保存數據到 LocalStorage
 *
 * @param key - 鍵名
 * @param value - 值（自動 JSON 序列化）
 */
export function localSave<T = unknown>(key: string, value: T): void {
  try {
    const serialized = JSON.stringify(value);
    localStorage.setItem(key, serialized);
  } catch (error) {
    console.error(`Failed to save to localStorage (key: ${key}):`, error);
  }
}

/**
 * 從 LocalStorage 讀取數據
 *
 * @param key - 鍵名
 * @returns 解析後的值，或 null
 */
export function localRead<T = unknown>(key: string): T | null {
  try {
    const serialized = localStorage.getItem(key);
    if (serialized === null) {
      return null;
    }
    return JSON.parse(serialized) as T;
  } catch (error) {
    console.error(`Failed to read from localStorage (key: ${key}):`, error);
    return null;
  }
}

/**
 * 從 LocalStorage 刪除數據
 *
 * @param key - 鍵名
 */
export function localRemove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch (error) {
    console.error(`Failed to remove from localStorage (key: ${key}):`, error);
  }
}

/**
 * 清空 LocalStorage
 */
export function localClear(): void {
  try {
    localStorage.clear();
  } catch (error) {
    console.error('Failed to clear localStorage:', error);
  }
}

/**
 * 檢查 LocalStorage 中是否存在某個鍵
 *
 * @param key - 鍵名
 * @returns 是否存在
 */
export function localHas(key: string): boolean {
  return localStorage.getItem(key) !== null;
}
