/**
 * Smart Enum Type Definitions
 *
 * Corresponds to Vue's types/smart-enum.d.ts
 */

export interface SmartEnumItem<T = number | string> {
  value: T;
  desc: string;
}

export interface SmartEnum<T = number | string> {
  [key: string]: SmartEnumItem<T>;
}

export interface SmartEnumWrapper {
  [enumName: string]: SmartEnum;
}
