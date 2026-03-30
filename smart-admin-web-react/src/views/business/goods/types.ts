/**
 * Goods Management Type Definitions
 * 商品管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 商品狀態枚舉
 */
export const GoodsStatusEnum = {
  /** 預約中 */
  APPOINTMENT: 1,
  /** 售賣中 */
  SELLING: 2,
  /** 售罄 */
  SOLD_OUT: 3,
} as const;
export type GoodsStatusEnum = (typeof GoodsStatusEnum)[keyof typeof GoodsStatusEnum];

/**
 * 商品 VO (View Object)
 * 用於列表展示
 */
export interface GoodsVO {
  /** 商品ID */
  goodsId: number;

  /** 商品分類ID */
  categoryId: number;

  /** 商品分類名稱 */
  categoryName?: string;

  /** 商品名稱 */
  goodsName: string;

  /** 商品狀態 */
  goodsStatus: GoodsStatusEnum;

  /** 產地（逗號分隔） */
  place: string;

  /** 價格 */
  price: number;

  /** 上架狀態 */
  shelvesFlag: boolean;

  /** 備註 */
  remark?: string;

  /** 創建時間 */
  createTime?: string;

  /** 更新時間 */
  updateTime?: string;
}

/**
 * 商品查詢表單
 */
export interface GoodsQueryForm {
  /** 商品分類ID */
  categoryId?: number;

  /** 搜尋關鍵字（商品名稱） */
  searchWord?: string;

  /** 商品狀態 */
  goodsStatus?: GoodsStatusEnum;

  /** 產地 */
  place?: string;

  /** 上架狀態（快速篩選）*/
  shelvesFlag?: boolean;

  /** 商品類型 */
  goodsType?: string;

  /** 頁碼 */
  pageNum?: number;

  /** 每頁數量 */
  pageSize?: number;

  /** 排序列表 */
  sortItemList?: Array<{
    column: string;
    isAsc: boolean;
  }>;
}

/**
 * 商品新增表單
 */
export interface GoodsAddForm {
  /** 商品分類ID */
  categoryId: number;

  /** 商品名稱 */
  goodsName: string;

  /** 商品狀態 */
  goodsStatus: GoodsStatusEnum;

  /** 產地（數組，多選） */
  place: string[];

  /** 價格 */
  price: number;

  /** 上架狀態 */
  shelvesFlag: boolean;

  /** 備註 */
  remark?: string;
}

/**
 * 商品更新表單
 */
export interface GoodsUpdateForm {
  /** 商品ID */
  goodsId: number;

  /** 商品分類ID */
  categoryId: number;

  /** 商品名稱 */
  goodsName: string;

  /** 商品狀態 */
  goodsStatus: GoodsStatusEnum;

  /** 產地（數組，多選） */
  place: string[];

  /** 價格 */
  price: number;

  /** 上架狀態 */
  shelvesFlag: boolean;

  /** 備註 */
  remark?: string;
}

/**
 * 商品表單數據（用於 Drawer）
 */
export interface GoodsFormData {
  /** 商品ID（編輯時存在） */
  goodsId?: number;

  /** 商品分類ID */
  categoryId?: number;

  /** 商品名稱 */
  goodsName?: string;

  /** 商品狀態 */
  goodsStatus?: GoodsStatusEnum;

  /** 產地（數組，多選） */
  place?: string[];

  /** 價格 */
  price?: number;

  /** 上架狀態 */
  shelvesFlag?: boolean;

  /** 備註 */
  remark?: string;
}
