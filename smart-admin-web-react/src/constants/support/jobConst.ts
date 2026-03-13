/**
 * Job Management Constants
 * 定時任務常量定義
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/support/job-const.js
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * 定時任務權限點
 */
export const JOB_PERMISSION = {
  /** 查詢 */
  QUERY: 'support:job:query',
  /** 新增 */
  ADD: 'support:job:add',
  /** 更新 */
  UPDATE: 'support:job:update',
  /** 刪除 */
  DELETE: 'support:job:delete',
  /** 執行 */
  EXECUTE: 'support:job:execute',
  /** 更新啟用狀態 */
  UPDATE_ENABLED: 'support:job:update:enabled',
  /** 查詢執行記錄 */
  LOG_QUERY: 'support:job:log:query',
} as const;

/**
 * 驗證規則
 */
export const JOB_VALIDATION = {
  /** 任務名稱最大長度 */
  NAME_MAX_LENGTH: 100,
  /** 執行類最大長度 */
  CLASS_MAX_LENGTH: 200,
  /** 觸發配置最大長度 */
  TRIGGER_VALUE_MAX_LENGTH: 200,
  /** 參數最大長度 */
  PARAM_MAX_LENGTH: 500,
  /** 備註最大長度 */
  REMARK_MAX_LENGTH: 500,
} as const;

/**
 * 觸發類型標籤映射
 */
export const JOB_TRIGGER_TYPE_LABELS = {
  CRON: 'CRON 表達式',
  FIXED_DELAY: '固定延遲',
  FIXED_RATE: '固定頻率',
} as const;

/**
 * 觸發類型顏色映射（Ant Design Tag color）
 */
export const JOB_TRIGGER_TYPE_COLORS = {
  CRON: 'success',
  FIXED_DELAY: 'processing',
  FIXED_RATE: 'warning',
} as const;

/**
 * 表格列寬度配置
 */
export const JOB_TABLE_COLUMNS_WIDTH = {
  jobId: 80,
  jobName: 150,
  jobClass: 180,
  triggerType: 110,
  triggerValue: 150,
  lastJob: 180,
  nextJob: 150,
  enabledFlag: 100,
  param: 150,
  remark: 200,
  sort: 80,
  updateName: 100,
  updateTime: 160,
  action: 200,
} as const;
