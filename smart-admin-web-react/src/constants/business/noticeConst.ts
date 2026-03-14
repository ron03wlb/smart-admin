/**
 * Notice Management Constants
 * 通知公告管理常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 通知公告管理權限點
 */
export const NOTICE_PERMISSION = {
  /** 查詢權限 */
  QUERY: 'oa:notice:query',

  /** 新增權限 */
  ADD: 'oa:notice:add',

  /** 更新權限 */
  UPDATE: 'oa:notice:update',

  /** 刪除權限 */
  DELETE: 'oa:notice:delete',
} as const;

/**
 * 通知公告驗證規則
 */
export const NOTICE_VALIDATION = {
  /** 標題最大長度 */
  TITLE_MAX_LENGTH: 100,

  /** 文號最大長度 */
  DOCUMENT_NUMBER_MAX_LENGTH: 50,

  /** 作者最大長度 */
  AUTHOR_MAX_LENGTH: 50,

  /** 來源最大長度 */
  SOURCE_MAX_LENGTH: 100,
} as const;

/**
 * 通知公告表格列寬度配置
 */
export const NOTICE_TABLE_COLUMNS_WIDTH = {
  /** 公告標題 */
  title: 300,

  /** 分類名稱 */
  noticeTypeName: 120,

  /** 文號 */
  documentNumber: 180,

  /** 作者 */
  author: 100,

  /** 來源 */
  source: 120,

  /** 可見範圍 */
  allVisibleFlag: 100,

  /** 發布時間 */
  publishTime: 180,

  /** 創建人 */
  createUserName: 100,

  /** 創建時間 */
  createTime: 180,

  /** 操作列 */
  operate: 150,
} as const;

/**
 * 可見範圍標籤
 */
export const VISIBLE_FLAG_LABELS = {
  true: '全部可見',
  false: '部分可見',
} as const;
