/**
 * Category Management Types
 * 分類管理類型定義
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/catalog/
 * 後端 VO：CategoryVO.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * 分類視圖對象（列表展示）
 */
export interface CategoryVO {
  /** 分類 ID */
  categoryId: number;
  /** 分類名稱 */
  categoryName: string;
  /** 分類類型 (1:商品分類 2:演示分類) */
  categoryType: number;
  /** 父級分類 ID */
  parentId?: number;
  /** 排序 */
  sort?: number;
  /** 備註 */
  remark?: string;
  /** 是否禁用 */
  disabledFlag: boolean;
  /** 創建時間 */
  createTime?: string;
  /** 更新時間 */
  updateTime?: string;
  /** 子分類列表（樹形結構） */
  children?: CategoryVO[];
}

/**
 * 分類查詢表單（樹形查詢）
 */
export interface CategoryTreeQueryForm {
  /** 分類類型（必填） */
  categoryType: number;
  /** 父級分類 ID（可選，用於查詢子分類） */
  parentId?: number;
}

/**
 * 分類新增表單
 */
export interface CategoryAddForm {
  /** 分類名稱 */
  categoryName: string;
  /** 分類類型 */
  categoryType: number;
  /** 父級分類 ID */
  parentId?: number;
  /** 排序 */
  sort?: number;
  /** 備註 */
  remark?: string;
}

/**
 * 分類更新表單
 */
export interface CategoryUpdateForm {
  /** 分類 ID（必填，用於標識要更新的分類） */
  categoryId: number;
  /** 分類名稱 */
  categoryName: string;
  /** 分類類型 */
  categoryType: number;
  /** 父級分類 ID */
  parentId?: number;
  /** 排序 */
  sort?: number;
  /** 備註 */
  remark?: string;
}

/**
 * 分類表單（用於 Modal，支持新增和編輯）
 */
export type CategoryFormData = Partial<CategoryAddForm> & {
  /** 分類 ID（編輯時有值，新增時為 undefined） */
  categoryId?: number;
};

/**
 * 分類類型枚舉
 */
export const CategoryTypeEnum = {
  /** 商品分類 */
  GOODS: 1,
  /** 演示分類 */
  DEMO: 2,
} as const;
export type CategoryTypeEnum = (typeof CategoryTypeEnum)[keyof typeof CategoryTypeEnum];

/**
 * 禁用狀態枚舉（使用 number 而非 boolean 以符合 TypeScript 枚舉規範）
 */
export const DisabledStatusEnum = {
  /** 啟用 */
  ENABLED: 0,
  /** 禁用 */
  DISABLED: 1,
} as const;
export type DisabledStatusEnum = (typeof DisabledStatusEnum)[keyof typeof DisabledStatusEnum];
