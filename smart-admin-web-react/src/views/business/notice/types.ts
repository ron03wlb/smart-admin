/**
 * Notice Management Type Definitions
 * 通知公告管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 通知公告 VO (View Object)
 * 用於列表展示
 */
export interface NoticeVO {
  /** 公告ID */
  noticeId: number;

  /** 公告標題 */
  title: string;

  /** 分類ID */
  noticeTypeId: number;

  /** 分類名稱 */
  noticeTypeName?: string;

  /** 文號 */
  documentNumber?: string;

  /** 作者 */
  author?: string;

  /** 來源 */
  source?: string;

  /** 是否全部可見 */
  allVisibleFlag: boolean;

  /** 發布時間 */
  publishTime?: string;

  /** 創建人姓名 */
  createUserName?: string;

  /** 創建時間 */
  createTime?: string;

  /** 更新時間 */
  updateTime?: string;

  /** 是否已刪除 */
  deletedFlag?: boolean;
}

/**
 * 通知公告查詢表單
 */
export interface NoticeQueryForm {
  /** 分類ID */
  noticeTypeId?: number;

  /** 關鍵字（標題、作者、來源） */
  keywords?: string;

  /** 文號 */
  documentNumber?: string;

  /** 創建人姓名 */
  createUserName?: string;

  /** 是否已刪除 */
  deletedFlag?: boolean;

  /** 發布時間開始 */
  publishTimeBegin?: string;

  /** 發布時間結束 */
  publishTimeEnd?: string;

  /** 創建時間開始 */
  createTimeBegin?: string;

  /** 創建時間結束 */
  createTimeEnd?: string;

  /** 頁碼 */
  pageNum?: number;

  /** 每頁數量 */
  pageSize?: number;
}

/**
 * 通知公告新增表單
 */
export interface NoticeAddForm {
  /** 公告標題 */
  title: string;

  /** 分類ID */
  noticeTypeId: number;

  /** 文號 */
  documentNumber?: string;

  /** 作者 */
  author: string;

  /** 來源 */
  source: string;

  /** 是否全部可見（1=全部可見, 0=部分可見） */
  allVisibleFlag: number;

  /** 發布時間 */
  publishTime?: string;

  /** 公告內容（HTML） */
  contentHtml: string;
}

/**
 * 通知公告更新表單
 */
export interface NoticeUpdateForm {
  /** 公告ID */
  noticeId: number;

  /** 公告標題 */
  title: string;

  /** 分類ID */
  noticeTypeId: number;

  /** 文號 */
  documentNumber?: string;

  /** 作者 */
  author: string;

  /** 來源 */
  source: string;

  /** 是否全部可見（1=全部可見, 0=部分可見） */
  allVisibleFlag: number;

  /** 發布時間 */
  publishTime?: string;

  /** 公告內容（HTML） */
  contentHtml: string;
}

/**
 * 通知公告表單數據（用於 Drawer）
 */
export interface NoticeFormData {
  /** 公告ID（編輯時存在） */
  noticeId?: number;

  /** 公告標題 */
  title?: string;

  /** 分類ID */
  noticeTypeId?: number;

  /** 文號 */
  documentNumber?: string;

  /** 作者 */
  author?: string;

  /** 來源 */
  source?: string;

  /** 是否全部可見（1=全部可見, 0=部分可見） */
  allVisibleFlag?: number;

  /** 發布時間 */
  publishTime?: string;

  /** 公告內容（HTML） */
  contentHtml?: string;
}

/**
 * 通知公告類型 VO
 */
export interface NoticeTypeVO {
  /** 類型ID */
  noticeTypeId: number;

  /** 類型名稱 */
  noticeTypeName: string;
}
