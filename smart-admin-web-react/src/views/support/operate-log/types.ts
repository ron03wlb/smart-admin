/**
 * Operate Log Types
 * 操作日誌類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * 操作日誌 VO
 */
export interface OperateLogVO {
  operateLogId: number;
  operateUserId: number;
  operateUserType: number;
  operateUserName: string;
  module: string;
  content: string;
  url: string;
  method: string;
  param?: string; // JSON 字符串
  response?: string; // JSON 字符串
  ip: string;
  ipRegion?: string;
  userAgent?: string;
  successFlag: number; // 0-失敗, 1-成功
  failReason?: string;
  createTime?: string;
  updateTime?: string;
  // 前端解析字段
  browser?: string;
  os?: string;
  device?: string;
}

/**
 * 操作日誌查詢表單
 */
export interface OperateLogQueryForm {
  searchWord?: string; // 搜索關鍵字（用戶名/模塊/操作內容）
  keywords?: string; // 操作關鍵字：模塊/操作內容
  requestKeywords?: string; // 請求關鍵字：請求地址/請求方法/請求參數/返回結果
  userName?: string;
  successFlag?: number; // 0-失敗, 1-成功, undefined-全部
  startDate?: string;
  endDate?: string;
  pageNum?: number;
  pageSize?: number;
}
