/**
 * Login Log Types
 * 登錄日誌類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 登錄日誌 VO（列表顯示）
 */
export interface LoginLogVO {
  /**
   * 登錄日誌 ID
   */
  loginLogId: number;
  /**
   * 用戶 ID
   */
  userId: number;
  /**
   * 用戶類型
   */
  userType: number;
  /**
   * 用戶名
   */
  userName: string;
  /**
   * 登錄 IP
   */
  loginIp: string;
  /**
   * 登錄 IP 地區
   */
  loginIpRegion?: string;
  /**
   * User-Agent
   */
  userAgent?: string;
  /**
   * 備註
   */
  remark?: string;
  /**
   * 登錄結果（0-成功, 1-失敗, 2-退出）
   */
  loginResult: number;
  /**
   * 登錄設備
   */
  loginDevice?: string;
  /**
   * 創建時間
   */
  createTime?: string;
  /**
   * 瀏覽器信息（前端解析 userAgent）
   */
  browser?: string;
  /**
   * 操作系統信息（前端解析 userAgent）
   */
  os?: string;
  /**
   * 設備信息（前端解析 userAgent）
   */
  device?: string;
}

/**
 * 登錄日誌查詢表單
 */
export interface LoginLogQueryForm {
  /**
   * 搜索關鍵字（用戶名/IP）
   */
  searchWord?: string;
  /**
   * 用戶 ID
   */
  userId?: number;
  /**
   * 用戶類型
   */
  userType?: number;
  /**
   * 開始日期
   */
  startDate?: string;
  /**
   * 結束日期
   */
  endDate?: string;
  /**
   * 用戶名稱
   */
  userName?: string;
  /**
   * IP
   */
  ip?: string;
  /**
   * 頁碼
   */
  pageNum?: number;
  /**
   * 每頁數量
   */
  pageSize?: number;
}
