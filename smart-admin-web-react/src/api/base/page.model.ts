/**
 * 分頁結果模型
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
export interface PageResult<T = any> {
  /**
   * 結果集
   */
  list: T[];

  /**
   * 總記錄數
   */
  total: number;

  /**
   * 當前頁
   */
  pageNum?: number;

  /**
   * 每頁數量
   */
  pageSize?: number;

  /**
   * 總頁數
   */
  pages?: number;

  /**
   * 是否為空
   */
  emptyFlag?: boolean;
}

/**
 * 分頁查詢參數
 */
export interface PageParam {
  /**
   * 頁碼（從 1 開始）
   */
  pageNum: number;

  /**
   * 每頁數量
   */
  pageSize: number;

  /**
   * 排序字段集合
   */
  sortItemList?: SortItem[];
}

/**
 * 排序項
 */
export interface SortItem {
  /**
   * 排序列
   */
  column: string;

  /**
   * 是否升序（true: 升序，false: 降序）
   */
  isAsc: boolean;
}
