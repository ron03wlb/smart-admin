/**
 * Job Management Types
 * 定時任務類型定義
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/job/job-list.vue
 * 後端 VO: smartadmin-support-job/.../domain/SmartJobVO.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

/**
 * Job VO
 * 定時任務視圖對象
 */
export interface JobVO {
  /** 任務 ID */
  jobId: number;
  /** 任務名稱 */
  jobName: string;
  /** 執行類 */
  jobClass: string;
  /** 觸發類型 */
  triggerType: string;
  /** 觸發類型描述 */
  triggerTypeDesc?: string;
  /** 觸發配置 */
  triggerValue: string;
  /** 定時任務參數 */
  param?: string;
  /** 是否啟用 */
  enabledFlag: boolean;
  /** 最後執行時間 */
  lastExecuteTime?: string;
  /** 最後執行記錄 ID */
  lastExecuteLogId?: number;
  /** 備註 */
  remark?: string;
  /** 排序 */
  sort?: number;
  /** 更新人 */
  updateName?: string;
  /** 更新時間 */
  updateTime?: string;
  /** 創建時間 */
  createTime?: string;
  /** 上次執行記錄 */
  lastJobLog?: JobLogVO;
  /** 未來 N 次任務執行時間 */
  nextJobExecuteTimeList?: string[];
  /** 啟用狀態加載中（UI 狀態） */
  enabledLoading?: boolean;
}

/**
 * Job Log VO
 * 任務執行記錄
 */
export interface JobLogVO {
  /** 記錄 ID */
  logId: number;
  /** 任務 ID */
  jobId: number;
  /** 任務名稱 */
  jobName?: string;
  /** 執行參數 */
  param?: string;
  /** 是否成功 (1:成功, 0:失敗) */
  successFlag: number;
  /** 執行開始時間 */
  executeStartTime?: string;
  /** 執行結束時間 */
  executeEndTime?: string;
  /** 執行用時(毫秒) */
  executeTimeMillis?: number;
  /** 執行結果 */
  executeResult?: string;
  /** IP地址 */
  ip?: string;
  /** 進程ID */
  processId?: string;
  /** 程序目錄 */
  programPath?: string;
  /** 執行人 */
  createName?: string;
  /** 創建時間 */
  createTime?: string;
}

/**
 * Job Query Form
 * 查詢表單
 */
export interface JobQueryForm {
  /** 關鍵字（任務名稱/執行類） */
  searchWord?: string;
  /** 啟用狀態 */
  enabledFlag?: boolean;
  /** 觸發類型 */
  triggerType?: string;
  /** 刪除標記 */
  deletedFlag?: boolean;
  /** 頁碼 */
  pageNum?: number;
  /** 每頁條數 */
  pageSize?: number;
}

/**
 * Job Add Form
 * 新增表單
 */
export interface JobAddForm {
  /** 任務名稱 */
  jobName: string;
  /** 執行類 */
  jobClass: string;
  /** 觸發類型 */
  triggerType: string;
  /** 觸發配置 */
  triggerValue: string;
  /** 定時任務參數 */
  param?: string;
  /** 是否啟用 */
  enabledFlag: boolean;
  /** 備註 */
  remark?: string;
  /** 排序 */
  sort?: number;
  /** 更新人 */
  updateName?: string;
}

/**
 * Job Update Form
 * 更新表單
 */
export interface JobUpdateForm extends JobAddForm {
  /** 任務 ID */
  jobId: number;
}

/**
 * Job Enabled Update Form
 * 啟用狀態更新表單
 */
export interface JobEnabledUpdateForm {
  /** 任務 ID */
  jobId: number;
  /** 是否啟用 */
  enabledFlag: boolean;
  /** 更新人 */
  updateName?: string;
}

/**
 * Job Execute Form
 * 立即執行表單
 */
export interface JobExecuteForm {
  /** 任務 ID */
  jobId: number;
  /** 更新人 */
  updateName?: string;
}

/**
 * Job Form Data (for Ant Design Form)
 * 表單數據（用於 Ant Design Form）
 */
export interface JobFormData {
  jobId?: number;
  jobName: string;
  jobClass: string;
  triggerType: string;
  triggerValue: string;
  param?: string;
  enabledFlag: boolean;
  remark?: string;
  sort?: number;
}

/**
 * Job Log Query Form
 * 執行記錄查詢表單
 */
export interface JobLogQueryForm {
  /** 任務ID (必填) */
  jobId: number;
  /** 搜索詞 (執行參數搜索) */
  searchWord?: string;
  /** 是否成功 (1:成功, 0:失敗) */
  successFlag?: number;
  /** 開始時間 (YYYY-MM-DD) */
  startTime?: string;
  /** 結束時間 (YYYY-MM-DD) */
  endTime?: string;
  /** 頁碼 */
  pageNum: number;
  /** 每頁條數 */
  pageSize: number;
}

/**
 * Job Trigger Type Enum
 * 觸發類型枚舉
 */
export enum JobTriggerTypeEnum {
  /** CRON 表達式 */
  CRON = 'CRON',
  /** 固定延遲 */
  FIXED_DELAY = 'FIXED_DELAY',
  /** 固定頻率 */
  FIXED_RATE = 'FIXED_RATE',
}
