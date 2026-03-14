/**
 * Goods Management Constants
 * 商品管理常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { GoodsStatusEnum } from '@/views/business/goods/types';

/**
 * 商品管理權限點
 */
export const GOODS_PERMISSION = {
  /** 查詢權限 */
  QUERY: 'goods:query',

  /** 新增權限 */
  ADD: 'goods:add',

  /** 更新權限 */
  UPDATE: 'goods:update',

  /** 刪除權限 */
  DELETE: 'goods:delete',

  /** 批量刪除權限 */
  BATCH_DELETE: 'goods:batchDelete',

  /** 導入權限 */
  IMPORT: 'goods:importGoods',

  /** 導出權限 */
  EXPORT: 'goods:exportGoods',
} as const;

/**
 * 商品驗證規則
 */
export const GOODS_VALIDATION = {
  /** 商品名稱最大長度 */
  NAME_MAX_LENGTH: 50,

  /** 備註最大長度 */
  REMARK_MAX_LENGTH: 200,

  /** 最小價格 */
  MIN_PRICE: 0,

  /** 最大價格 */
  MAX_PRICE: 999999,
} as const;

/**
 * 商品表格列寬度配置
 */
export const GOODS_TABLE_COLUMNS_WIDTH = {
  /** 商品分類 */
  categoryName: 150,

  /** 商品名稱 */
  goodsName: 150,

  /** 商品狀態 */
  goodsStatus: 150,

  /** 產地 */
  place: 150,

  /** 價格 */
  price: 100,

  /** 上架狀態 */
  shelvesFlag: 80,

  /** 備註 */
  remark: 150,

  /** 創建時間 */
  createTime: 150,

  /** 操作列 */
  operate: 150,
} as const;

/**
 * 商品狀態標籤
 */
export const GOODS_STATUS_LABELS = {
  [GoodsStatusEnum.APPOINTMENT]: '預約中',
  [GoodsStatusEnum.SELLING]: '售賣中',
  [GoodsStatusEnum.SOLD_OUT]: '售罄',
} as const;

/**
 * 商品狀態顏色
 */
export const GOODS_STATUS_COLORS = {
  [GoodsStatusEnum.APPOINTMENT]: 'blue',
  [GoodsStatusEnum.SELLING]: 'green',
  [GoodsStatusEnum.SOLD_OUT]: 'red',
} as const;

/**
 * 上架狀態標籤
 */
export const SHELVES_FLAG_LABELS = {
  true: '上架',
  false: '下架',
} as const;
