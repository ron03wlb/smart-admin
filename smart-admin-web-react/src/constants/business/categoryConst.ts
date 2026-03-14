/**
 * Category Management Constants
 * 分類管理常量定義
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/business/erp/category-const.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * 分類管理權限點
 */
export const CATEGORY_PERMISSION = {
  /** 添加分類 */
  ADD: 'business:category:add',
  /** 添加子分類 */
  ADD_CHILD: 'business:category:addChild',
  /** 更新分類 */
  UPDATE: 'business:category:update',
  /** 刪除分類 */
  DELETE: 'business:category:delete',
} as const;

/**
 * 驗證規則
 */
export const CATEGORY_VALIDATION = {
  /** 分類名稱最大長度 */
  NAME_MAX_LENGTH: 30,
  /** 備註最大長度 */
  REMARK_MAX_LENGTH: 200,
} as const;

/**
 * 分類類型標籤映射
 */
export const CATEGORY_TYPE_LABELS = {
  1: '商品分類',
  2: '演示分類',
} as const;

/**
 * 禁用狀態標籤映射
 */
export const DISABLED_STATUS_LABELS = {
  false: '啟用',
  true: '禁用',
} as const;

/**
 * 表格列寬度配置
 */
export const CATEGORY_TABLE_COLUMNS_WIDTH = {
  categoryName: 200,
  categoryType: 100,
  sort: 80,
  disabledFlag: 80,
  remark: 200,
  createTime: 160,
  updateTime: 160,
  action: 220,
} as const;
