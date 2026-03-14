/**
 * Position Types
 * 職位類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 職位 VO（列表顯示）
 */
export interface PositionVO {
  /**
   * 職位 ID
   */
  positionId: number;
  /**
   * 職位名稱
   */
  positionName: string;
  /**
   * 職級
   */
  positionLevel?: string;
  /**
   * 排序
   */
  sort: number;
  /**
   * 備註
   */
  remark?: string;
  /**
   * 創建時間
   */
  createTime?: string;
  /**
   * 更新時間
   */
  updateTime?: string;
}

/**
 * 職位表單數據（新增/編輯）
 */
export interface PositionFormData {
  /**
   * 職位 ID（編輯時有值）
   */
  positionId?: number;
  /**
   * 職位名稱
   */
  positionName: string;
  /**
   * 職級
   */
  positionLevel?: string;
  /**
   * 排序
   */
  sort?: number;
  /**
   * 備註
   */
  remark?: string;
}

/**
 * 職位查詢表單
 */
export interface PositionQueryForm {
  /**
   * 關鍵字查詢
   */
  keywords?: string;
  /**
   * 頁碼
   */
  pageNum: number;
  /**
   * 每頁數量
   */
  pageSize: number;
}
