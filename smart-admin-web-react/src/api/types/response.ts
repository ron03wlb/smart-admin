/**
 * SmartAdmin ResponseDTO 類型定義
 * 統一後端 API 響應格式
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

/**
 * 標準響應結構
 * @template T 數據類型
 */
export interface ResponseDTO<T = any> {
  /** 響應碼（200 成功，其他為錯誤碼） */
  code: number;

  /** 響應消息 */
  msg: string;

  /** 是否成功 */
  ok: boolean;

  /** 響應數據 */
  data: T;

  /** 服務器時間戳 */
  timestamp?: number;
}

/**
 * 分頁結果
 * @template T 列表數據類型
 */
export interface PageResult<T = any> {
  /** 當前頁碼 */
  pageNum: number;

  /** 每頁數量 */
  pageSize: number;

  /** 總記錄數 */
  total: number;

  /** 總頁數 */
  pages: number;

  /** 數據列表 */
  list: T[];

  /** 是否為空 */
  emptyFlag: boolean;
}

/**
 * 分頁響應（包含 ResponseDTO 和 PageResult）
 * @template T 列表數據類型
 */
export type PageResponseDTO<T = any> = ResponseDTO<PageResult<T>>;

/**
 * 選項數據結構（用於下拉選擇等）
 */
export interface OptionVO {
  /** 選項值 */
  value: string | number;

  /** 選項標籤 */
  label: string;

  /** 是否禁用 */
  disabled?: boolean;

  /** 擴展數據 */
  [key: string]: any;
}
