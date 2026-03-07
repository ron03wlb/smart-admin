/**
 * Smart Enum Library
 *
 * React version of Vue's plugins/smart-enums-plugin.ts.
 * Instead of a Vue plugin, this exports pure functions that can be imported directly.
 */
import allEnums from '@/constants';
import { FLAG_NUMBER_ENUM } from '@/constants/common-const';
import type { SmartEnumItem } from '@/types/smart-enum.types';

/**
 * Get description by enum name and value.
 * Handles boolean → FLAG_NUMBER_ENUM conversion automatically.
 */
export function getDescByValue(enumName: string, value: string | number | boolean | null | undefined): string {
  if (!(enumName in allEnums)) {
    console.error(`Cannot find enum: ${enumName}. Check constants/index.ts for registration.`);
    return '';
  }

  let resolvedValue: string | number | null | undefined = value as string | number;

  // Boolean → FLAG_NUMBER_ENUM conversion
  if (enumName === 'FLAG_NUMBER_ENUM' && typeof value === 'boolean') {
    resolvedValue = value ? FLAG_NUMBER_ENUM.TRUE.value : FLAG_NUMBER_ENUM.FALSE.value;
  }

  const smartEnum = allEnums[enumName];
  for (const key in smartEnum) {
    if (smartEnum[key].value === resolvedValue) {
      return smartEnum[key].desc;
    }
  }
  return '';
}

/**
 * Get list of {value, desc} items for an enum.
 */
export function getValueDescList<T = number | string>(enumName: string): SmartEnumItem<T>[] {
  if (!(enumName in allEnums)) {
    console.error(`Cannot find enum: ${enumName}. Check constants/index.ts for registration.`);
    return [];
  }

  const result: SmartEnumItem<T>[] = [];
  const smartEnum = allEnums[enumName];
  for (const key in smartEnum) {
    result.push(smartEnum[key] as unknown as SmartEnumItem<T>);
  }
  return result;
}

/**
 * Get {value: desc} mapping object for an enum.
 */
export function getValueDesc(enumName: string): Record<string, string> {
  if (!(enumName in allEnums)) {
    console.error(`Cannot find enum: ${enumName}. Check constants/index.ts for registration.`);
    return {};
  }

  const smartEnum = allEnums[enumName];
  const result: Record<string, string> = {};
  for (const key in smartEnum) {
    result[String(smartEnum[key].value)] = smartEnum[key].desc;
  }
  return result;
}
