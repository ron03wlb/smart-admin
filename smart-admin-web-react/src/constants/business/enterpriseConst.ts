/**
 * Enterprise Management Constants
 * 企業管理常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { EnterpriseTypeEnum } from '@/views/business/enterprise/types';

/**
 * 企業管理權限點
 */
export const ENTERPRISE_PERMISSION = {
  /** 查詢權限 */
  QUERY: 'oa:enterprise:query',

  /** 新增權限 */
  ADD: 'oa:enterprise:add',

  /** 更新權限 */
  UPDATE: 'oa:enterprise:update',

  /** 刪除權限 */
  DELETE: 'oa:enterprise:delete',

  /** 查看詳情權限 */
  DETAIL: 'oa:enterprise:detail',

  /** 導出Excel權限 */
  EXPORT_EXCEL: 'oa:enterprise:exportExcel',
} as const;

/**
 * 企業驗證規則
 */
export const ENTERPRISE_VALIDATION = {
  /** 企業名稱最大長度 */
  NAME_MAX_LENGTH: 100,

  /** 統一社會信用代碼長度（18位） */
  CREDIT_CODE_LENGTH: 18,

  /** 聯系人最大長度 */
  CONTACT_MAX_LENGTH: 50,

  /** 電話最大長度 */
  PHONE_MAX_LENGTH: 20,

  /** 郵箱最大長度 */
  EMAIL_MAX_LENGTH: 100,

  /** 地址最大長度 */
  ADDRESS_MAX_LENGTH: 200,
} as const;

/**
 * 企業類型標籤
 */
export const ENTERPRISE_TYPE_LABELS = {
  [EnterpriseTypeEnum.LIMITED_LIABILITY]: '有限責任公司',
  [EnterpriseTypeEnum.JOINT_STOCK]: '股份有限公司',
  [EnterpriseTypeEnum.SOLE_PROPRIETORSHIP]: '個人獨資企業',
  [EnterpriseTypeEnum.PARTNERSHIP]: '合夥企業',
  [EnterpriseTypeEnum.OTHER]: '其他',
} as const;

/**
 * 企業類型顏色
 */
export const ENTERPRISE_TYPE_COLORS = {
  [EnterpriseTypeEnum.LIMITED_LIABILITY]: 'blue',
  [EnterpriseTypeEnum.JOINT_STOCK]: 'green',
  [EnterpriseTypeEnum.SOLE_PROPRIETORSHIP]: 'orange',
  [EnterpriseTypeEnum.PARTNERSHIP]: 'purple',
  [EnterpriseTypeEnum.OTHER]: 'default',
} as const;

/**
 * 企業表格列寬度配置
 */
export const ENTERPRISE_TABLE_COLUMNS_WIDTH = {
  /** 企業名稱 */
  enterpriseName: 180,

  /** 統一社會信用代碼 */
  unifiedSocialCreditCode: 170,

  /** 企業類型 */
  type: 120,

  /** 聯系人 */
  contact: 100,

  /** 聯系電話 */
  contactPhone: 130,

  /** 郵箱 */
  email: 180,

  /** 地址 */
  address: 250,

  /** 狀態 */
  disabledFlag: 80,

  /** 創建時間 */
  createTime: 180,

  /** 操作列 */
  action: 150,
} as const;

/**
 * 禁用狀態標籤
 */
export const DISABLED_FLAG_LABELS = {
  true: '禁用',
  false: '啟用',
} as const;

/**
 * 禁用狀態顏色
 */
export const DISABLED_FLAG_COLORS = {
  true: 'red',
  false: 'green',
} as const;
