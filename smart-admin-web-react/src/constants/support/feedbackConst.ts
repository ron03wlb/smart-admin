/**
 * Feedback Constants
 * 意見反饋常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 意見反饋權限點
 */
export const FEEDBACK_PERMISSION = {
  QUERY: 'support:feedback:query',
} as const;

/**
 * 意見反饋驗證規則
 */
export const FEEDBACK_VALIDATION = {
  SEARCH_WORD_MAX_LENGTH: 25,
  CONTENT_MAX_LENGTH: 500,
} as const;

/**
 * 意見反饋表格列寬度
 */
export const FEEDBACK_TABLE_COLUMNS_WIDTH = {
  feedbackId: 80,
  feedbackContent: 300,
  feedbackAttachment: 150,
  userName: 100,
  userType: 100,
  createTime: 180,
} as const;
