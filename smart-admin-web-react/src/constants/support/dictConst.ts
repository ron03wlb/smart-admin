/**
 * Dictionary Constants
 * 字典常量
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/support/dict-const.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

/**
 * Dictionary split character
 * 字典分隔符（用於多選字典值的分隔）
 *
 * 示例：
 * - dataValue: "1,2,3"
 * - dataLabel: "選項1,選項2,選項3"
 */
export const DICT_SPLIT = ',';

/**
 * 字典權限點
 */
export const DICT_PERMISSION = {
  ADD: 'support:dict:add',
  UPDATE: 'support:dict:update',
  DELETE: 'support:dict:delete',
} as const;

/**
 * 字典值權限點
 */
export const DICT_DATA_PERMISSION = {
  ADD: 'support:dictData:add',
  UPDATE: 'support:dictData:update',
  DELETE: 'support:dictData:delete',
} as const;

/**
 * 字典表格列寬度
 */
export const DICT_TABLE_COLUMNS_WIDTH = {
  dictId: 90,
  dictCode: 200,
  dictName: 200,
  remark: 250,
  disabledFlag: 90,
  updateTime: 160,
  action: 80,
} as const;

/**
 * 字典值表格列寬度
 */
export const DICT_DATA_TABLE_COLUMNS_WIDTH = {
  dataValue: 150,
  dataLabel: 150,
  disabledFlag: 90,
  sortOrder: 80,
  remark: 200,
  updateTime: 150,
  action: 80,
} as const;

/**
 * Dictionary Code Enum
 * 字典代碼枚舉
 *
 * 該常量來自於字典管理中的數據，統一引用便於維護
 *
 * 使用示例：
 * ```tsx
 * <DictSelect dictCode={DICT_CODE_ENUM.GOODS_PLACE} />
 * ```
 */
export const DICT_CODE_ENUM = {
  /**
   * 商品產地字典
   */
  GOODS_PLACE: 'GOODS_PLACE',
} as const;

export type DictCodeEnum = (typeof DICT_CODE_ENUM)[keyof typeof DICT_CODE_ENUM];
