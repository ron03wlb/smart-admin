/**
 * Serial Number Types
 * 單號生成器類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 單號定義 VO
 */
export interface SerialNumberVO {
  serialNumberId: number;
  businessName: string; // 業務名稱
  format: string; // 格式
  ruleType: string; // 循環週期
  initNumber: number; // 初始值
  stepRandomRange: number; // 隨機增量
  remark?: string; // 備註
  lastNumber?: string; // 上次產生單號
  lastTime?: string; // 上次產生時間
}

/**
 * 生成單號表單
 */
export interface SerialNumberGenerateForm {
  serialNumberId: number;
  count: number; // 生成數量
}

/**
 * 生成記錄查詢表單
 */
export interface SerialNumberRecordQueryForm {
  serialNumberId: number;
  pageNum: number;
  pageSize: number;
}

/**
 * 生成記錄 VO
 */
export interface SerialNumberRecordVO {
  serialNumberId: number;
  recordDate: string; // 日期
  count: number; // 生成數量
  lastNumber: number; // 最後更新值
  lastTime?: string; // 上次生成時間
}
