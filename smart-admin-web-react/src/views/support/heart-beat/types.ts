/**
 * Heart Beat Types
 * 心跳記錄類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 心跳記錄 VO
 */
export interface HeartBeatVO {
  heartBeatId: number;
  projectPath: string;
  serverIp: string;
  processNo: string;
  processStartTime: string;
  heartBeatTime: string;
}

/**
 * 心跳記錄查詢表單
 */
export interface HeartBeatQueryForm {
  keywords?: string;
  startDate?: string | null;
  endDate?: string | null;
  pageNum: number;
  pageSize: number;
}
