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
