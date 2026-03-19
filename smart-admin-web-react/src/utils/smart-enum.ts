/**
 * SmartEnum Utility Functions
 * 枚舉工具函數
 *
 * 參考：Vue 版本 smart-admin-web/src/plugins/smart-enums-plugin.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { SmartEnum, SmartEnumItem, SmartEnumWrapper } from '@/types/smart-enum';
import * as COMMON_CONST from '@/constants/common-const';
import { CHANGE_LOG_TYPE_ENUM } from '@/constants/support/changeLogConst';

// 所有枚舉常量的集合
const SMART_ENUM_WRAPPER: SmartEnumWrapper = {
  FLAG_NUMBER_ENUM: COMMON_CONST.FLAG_NUMBER_ENUM,
  GENDER_ENUM: COMMON_CONST.GENDER_ENUM,
  USER_TYPE_ENUM: COMMON_CONST.USER_TYPE_ENUM,
  DATA_TYPE_ENUM: COMMON_CONST.DATA_TYPE_ENUM,
  USER_STATUS_ENUM: COMMON_CONST.USER_STATUS_ENUM,
  DELETED_FLAG_ENUM: COMMON_CONST.DELETED_FLAG_ENUM,
  CHANGE_LOG_TYPE_ENUM: CHANGE_LOG_TYPE_ENUM,
};

/**
 * 根據枚舉值獲取描述
 * @param constantName 枚舉名
 * @param value 枚舉值
 * @returns 描述文本
 */
export function getDescByValue<T = number | string>(
  constantName: string,
  value: T | undefined | null
): string {
  if (!SMART_ENUM_WRAPPER[constantName]) {
    console.error(`無法找到枚舉：${constantName}，請檢查是否已在 smart-enum.ts 中註冊！`);
    return '';
  }

  // boolean類型需要做特殊處理
  if (constantName === 'FLAG_NUMBER_ENUM' && typeof value === 'boolean') {
    value = (value ? 1 : 0) as T;
  }

  const smartEnum = SMART_ENUM_WRAPPER[constantName];

  for (const key in smartEnum) {
    if (smartEnum[key].value === value) {
      return smartEnum[key].desc;
    }
  }

  return '';
}

/**
 * 根據枚舉名獲取對應的值描述列表 [{value, desc}]
 * @param constantName 枚舉名
 * @returns 值描述列表
 */
export function getValueDescList<T = number | string>(constantName: string): SmartEnumItem<T>[] {
  if (!SMART_ENUM_WRAPPER[constantName]) {
    console.error(`無法找到枚舉：${constantName}，請檢查是否已在 smart-enum.ts 中註冊！`);
    return [];
  }

  const result: SmartEnumItem<T>[] = [];
  const targetSmartEnum = SMART_ENUM_WRAPPER[constantName];

  for (const key in targetSmartEnum) {
    result.push(targetSmartEnum[key] as SmartEnumItem<T>);
  }

  return result;
}

/**
 * 根據枚舉名獲取對應的值描述對象 {value: desc}
 * @param constantName 枚舉名
 * @returns 值描述對象
 */
export function getValueDesc(constantName: string): Record<string, string> {
  if (!SMART_ENUM_WRAPPER[constantName]) {
    console.error(`無法找到枚舉：${constantName}，請檢查是否已在 smart-enum.ts 中註冊！`);
    return {};
  }

  const smartEnum = SMART_ENUM_WRAPPER[constantName];
  const result: Record<string, string> = {};

  for (const key in smartEnum) {
    const valueKey = String(smartEnum[key].value);
    result[valueKey] = smartEnum[key].desc;
  }

  return result;
}

/**
 * 註冊新的枚舉（動態添加）
 * @param constantName 枚舉名
 * @param smartEnum 枚舉對象
 */
export function registerEnum(constantName: string, smartEnum: SmartEnum): void {
  if (SMART_ENUM_WRAPPER[constantName]) {
    console.warn(`枚舉 ${constantName} 已存在，將被覆蓋！`);
  }
  SMART_ENUM_WRAPPER[constantName] = smartEnum;
}

/**
 * 獲取所有已註冊的枚舉名稱
 * @returns 枚舉名稱列表
 */
export function getAllEnumNames(): string[] {
  return Object.keys(SMART_ENUM_WRAPPER);
}
