/**
 * Brand Module Constants
 * 品牌模塊常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { BrandStatusEnum } from './types';

/**
 * Brand Permission Points
 * 品牌權限點
 */
export const BRAND_PERMISSIONS = {
  QUERY: 'business:brand:query', // 查詢
  ADD: 'business:brand:add', // 新增
  UPDATE: 'business:brand:update', // 更新
  BATCH_DELETE: 'business:brand:batchDelete', // 批量刪除
} as const;

/**
 * Brand Status Labels
 * 品牌狀態標籤
 */
export const BRAND_STATUS_LABELS: Record<BrandStatusEnum, string> = {
  [BrandStatusEnum.DISABLED]: '禁用',
  [BrandStatusEnum.ENABLED]: '啟用',
};

/**
 * Brand Status Tag Colors
 * 品牌狀態標籤顏色
 */
export const BRAND_STATUS_TAG_COLORS: Record<BrandStatusEnum, string> = {
  [BrandStatusEnum.DISABLED]: 'red',
  [BrandStatusEnum.ENABLED]: 'green',
};

/**
 * Brand Status Options
 * 品牌狀態選項
 */
export const BRAND_STATUS_OPTIONS = [
  { value: BrandStatusEnum.ENABLED, label: BRAND_STATUS_LABELS[BrandStatusEnum.ENABLED] },
  { value: BrandStatusEnum.DISABLED, label: BRAND_STATUS_LABELS[BrandStatusEnum.DISABLED] },
];

/**
 * Brand Form Validation Rules
 * 品牌表單驗證規則
 */
export const BRAND_FORM_RULES = {
  brandName: [
    { required: true, message: '請輸入品牌名稱' },
    { max: 50, message: '品牌名稱不能超過50個字符' },
  ],
  brandLogo: [{ max: 200, message: 'Logo URL不能超過200個字符' }],
  description: [{ max: 500, message: '描述不能超過500個字符' }],
  sort: [
    { required: true, message: '請輸入排序值' },
    { type: 'number', message: '排序值必須為數字' },
  ],
  status: [{ required: true, message: '請選擇狀態' }],
} as const;

/**
 * Brand Table Column Widths
 * 品牌表格列寬配置
 */
export const BRAND_COLUMN_WIDTHS = {
  brandId: 80,
  brandName: 150,
  brandLogo: 100,
  description: 200,
  sort: 80,
  status: 80,
  updateTime: 160,
  createTime: 160,
  actions: 180,
} as const;
