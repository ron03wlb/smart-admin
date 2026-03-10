/**
 * SmartEnum Type Definitions
 * 枚舉類型定義
 *
 * 參考：Vue 版本 smart-admin-web/src/types/smart-enum.d.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

export interface SmartEnumItem<T = number | string> {
  value: T;
  desc: string;
}

export interface SmartEnum<T = number | string> {
  [key: string]: SmartEnumItem<T>;
}

export interface SmartEnumWrapper {
  [key: string]: SmartEnum;
}
