/**
 * ChangeLog Management Constants
 * 系統更新日誌常量定義
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/change-log/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import type { SmartEnum } from '@/types/smart-enum';

/**
 * 更新類型枚舉（SmartEnum 格式，用於 SmartEnumSelect）
 */
export const CHANGE_LOG_TYPE_ENUM: SmartEnum<number> = {
  MAJOR: {
    value: 1,
    desc: '重大更新',
  },
  FEATURE: {
    value: 2,
    desc: '功能更新',
  },
  BUGFIX: {
    value: 3,
    desc: 'Bug修復',
  },
};

/**
 * 系統更新日誌權限點
 */
export const CHANGE_LOG_PERMISSION = {
  /** 查詢 */
  QUERY: 'support:changeLog:query',
  /** 新增 */
  ADD: 'support:changeLog:add',
  /** 更新 */
  UPDATE: 'support:changeLog:update',
  /** 刪除 */
  DELETE: 'support:changeLog:delete',
  /** 批量刪除 */
  BATCH_DELETE: 'support:changeLog:batchDelete',
} as const;

/**
 * 驗證規則
 */
export const CHANGE_LOG_VALIDATION = {
  /** 版本號最大長度 */
  VERSION_MAX_LENGTH: 50,
  /** 發布人最大長度 */
  AUTHOR_MAX_LENGTH: 50,
  /** 更新內容最大長度 */
  CONTENT_MAX_LENGTH: 2000,
  /** 跳轉鏈接最大長度 */
  LINK_MAX_LENGTH: 500,
} as const;

/**
 * 更新類型標籤映射
 */
export const CHANGE_LOG_TYPE_LABELS = {
  1: '重大更新',
  2: '功能更新',
  3: 'Bug修復',
} as const;

/**
 * 更新類型顏色映射（Ant Design Tag color）
 */
export const CHANGE_LOG_TYPE_COLORS = {
  1: 'red', // 重大更新 - 紅色
  2: 'blue', // 功能更新 - 藍色
  3: 'green', // Bug修復 - 綠色
} as const;

/**
 * 表格列寬度配置
 */
export const CHANGE_LOG_TABLE_COLUMNS_WIDTH = {
  updateVersion: 120,
  type: 100,
  publishAuthor: 100,
  publicDate: 120,
  content: 300,
  link: 200,
  createTime: 160,
  updateTime: 160,
  action: 140,
} as const;
