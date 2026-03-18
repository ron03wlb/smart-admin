/**
 * Feedback Types
 * 意見反饋類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 意見反饋 VO（列表顯示）
 */
export interface FeedbackVO {
  /**
   * 主鍵
   */
  feedbackId: number;
  /**
   * 反饋內容
   */
  feedbackContent: string;
  /**
   * 反饋圖片（JSON 字符串）
   */
  feedbackAttachment?: string;
  /**
   * 創建人 ID
   */
  userId: number;
  /**
   * 創建人姓名
   */
  userName: string;
  /**
   * 創建人類型
   */
  userType: number;
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
 * 意見反饋查詢表單
 */
export interface FeedbackQueryForm {
  /**
   * 搜索詞（反饋內容/創建人）
   */
  searchWord?: string;
  /**
   * 開始時間
   */
  startDate?: string;
  /**
   * 截止時間
   */
  endDate?: string;
  /**
   * 頁碼
   */
  pageNum?: number;
  /**
   * 每頁數量
   */
  pageSize?: number;
}

/**
 * 意見反饋新增表單
 * 注意：此表單通常由用戶端使用，管理端只查看
 */
export interface FeedbackAddForm {
  /**
   * 反饋內容
   */
  feedbackContent: string;
  /**
   * 反饋圖片
   */
  feedbackAttachment?: string;
}
