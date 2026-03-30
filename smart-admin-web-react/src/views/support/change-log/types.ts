/**
 * ChangeLog Management Types
 * 系統更新日誌類型定義
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/change-log/change-log-list.vue
 * 後端 VO: smartadmin-support-changelog/.../domain/vo/ChangeLogVO.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * ChangeLog VO
 * 系統更新日誌視圖對象
 */
export interface ChangeLogVO {
  /** 更新日誌 ID */
  changeLogId: number;
  /** 版本號 */
  updateVersion: string;
  /** 更新類型 */
  type: number;
  /** 發布人 */
  publishAuthor: string;
  /** 發布日期 */
  publicDate: string;
  /** 更新內容 */
  content: string;
  /** 跳轉鏈接 */
  link?: string;
  /** 創建時間 */
  createTime?: string;
  /** 更新時間 */
  updateTime?: string;
}

/**
 * ChangeLog Query Form
 * 查詢表單
 */
export interface ChangeLogQueryForm {
  /** 更新類型 */
  type?: number;
  /** 關鍵字 */
  keyword?: string;
  /** 發布日期開始 */
  publicDateBegin?: string;
  /** 發布日期結束 */
  publicDateEnd?: string;
  /** 創建時間 */
  createTime?: string;
  /** 跳轉鏈接 */
  link?: string;
  /** 頁碼 */
  pageNum?: number;
  /** 每頁條數 */
  pageSize?: number;
  /** 是否查詢總記錄數（分頁優化） */
  searchCount?: boolean;
}

/**
 * ChangeLog Add Form
 * 新增表單
 */
export interface ChangeLogAddForm {
  /** 版本號 */
  updateVersion: string;
  /** 更新類型 */
  type: number;
  /** 發布人 */
  publishAuthor: string;
  /** 發布日期 */
  publicDate: string;
  /** 更新內容 */
  content: string;
  /** 跳轉鏈接 */
  link?: string;
}

/**
 * ChangeLog Update Form
 * 更新表單
 */
export interface ChangeLogUpdateForm extends ChangeLogAddForm {
  /** 更新日誌 ID */
  changeLogId: number;
}

/**
 * ChangeLog Form Data (for Ant Design Form)
 * 表單數據（用於 Ant Design Form）
 */
export interface ChangeLogFormData {
  changeLogId?: number;
  updateVersion: string;
  type: number;
  publishAuthor: string;
  publicDate: any; // Dayjs object from DatePicker
  content: string;
  link?: string;
}

/**
 * ChangeLog Type Enum
 * 更新類型枚舉：1-重大更新, 2-功能更新, 3-Bug修復
 */
export const ChangeLogTypeEnum = {
  /** 重大更新 */
  MAJOR_UPDATE: 1,
  /** 功能更新 */
  FUNCTION_UPDATE: 2,
  /** Bug修復 */
  BUG_FIX: 3,
} as const;
export type ChangeLogTypeEnum = (typeof ChangeLogTypeEnum)[keyof typeof ChangeLogTypeEnum];
