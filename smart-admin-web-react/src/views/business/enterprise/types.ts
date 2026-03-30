/**
 * Enterprise Management Type Definitions
 * 企業管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 企業類型枚舉
 */
export const EnterpriseTypeEnum = {
  /** 有限責任公司 */
  LIMITED_LIABILITY: 1,
  /** 股份有限公司 */
  JOINT_STOCK: 2,
  /** 個人獨資企業 */
  SOLE_PROPRIETORSHIP: 3,
  /** 合夥企業 */
  PARTNERSHIP: 4,
  /** 其他 */
  OTHER: 5,
} as const;
export type EnterpriseTypeEnum = (typeof EnterpriseTypeEnum)[keyof typeof EnterpriseTypeEnum];

/**
 * 企業 VO (View Object)
 * 用於列表展示
 */
export interface EnterpriseVO {
  /** 企業ID */
  enterpriseId: number;

  /** 企業名稱 */
  enterpriseName: string;

  /** 統一社會信用代碼 */
  unifiedSocialCreditCode: string;

  /** 企業類型 */
  type: EnterpriseTypeEnum;

  /** 聯系人 */
  contact: string;

  /** 聯系電話 */
  contactPhone: string;

  /** 郵箱 */
  email?: string;

  /** 省份代碼 */
  province?: string;

  /** 省份名稱 */
  provinceName?: string;

  /** 城市代碼 */
  city?: string;

  /** 城市名稱 */
  cityName?: string;

  /** 區縣代碼 */
  district?: string;

  /** 區縣名稱 */
  districtName?: string;

  /** 詳細地址 */
  address?: string;

  /** 企業 Logo URL */
  enterpriseLogo?: string;

  /** 營業執照 URL */
  businessLicense?: string;

  /** 是否禁用 */
  disabledFlag: boolean;

  /** 創建時間 */
  createTime?: string;

  /** 更新時間 */
  updateTime?: string;
}

/**
 * 企業查詢表單
 */
export interface EnterpriseQueryForm {
  /** 關鍵字（企業名稱/聯系人/聯系電話） */
  keywords?: string;

  /** 創建時間開始 */
  createTimeBegin?: string;

  /** 創建時間結束 */
  createTimeEnd?: string;

  /** 頁碼 */
  pageNum?: number;

  /** 每頁數量 */
  pageSize?: number;
}

/**
 * 企業新增表單
 */
export interface EnterpriseAddForm {
  /** 企業名稱 */
  enterpriseName: string;

  /** 統一社會信用代碼 */
  unifiedSocialCreditCode: string;

  /** 企業類型 */
  type: EnterpriseTypeEnum;

  /** 聯系人 */
  contact: string;

  /** 聯系電話 */
  contactPhone: string;

  /** 郵箱 */
  email?: string;

  /** 省份代碼 */
  province?: string;

  /** 城市代碼 */
  city?: string;

  /** 區縣代碼 */
  district?: string;

  /** 詳細地址 */
  address?: string;

  /** 企業 Logo URL */
  enterpriseLogo?: string;

  /** 營業執照 URL */
  businessLicense?: string;

  /** 是否禁用 */
  disabledFlag: boolean;
}

/**
 * 企業更新表單
 */
export interface EnterpriseUpdateForm {
  /** 企業ID */
  enterpriseId: number;

  /** 企業名稱 */
  enterpriseName: string;

  /** 統一社會信用代碼 */
  unifiedSocialCreditCode: string;

  /** 企業類型 */
  type: EnterpriseTypeEnum;

  /** 聯系人 */
  contact: string;

  /** 聯系電話 */
  contactPhone: string;

  /** 郵箱 */
  email?: string;

  /** 省份代碼 */
  province?: string;

  /** 城市代碼 */
  city?: string;

  /** 區縣代碼 */
  district?: string;

  /** 詳細地址 */
  address?: string;

  /** 企業 Logo URL */
  enterpriseLogo?: string;

  /** 營業執照 URL */
  businessLicense?: string;

  /** 是否禁用 */
  disabledFlag: boolean;
}

/**
 * 企業表單數據（用於 Modal）
 */
export interface EnterpriseFormData {
  /** 企業ID（編輯時存在） */
  enterpriseId?: number;

  /** 企業名稱 */
  enterpriseName?: string;

  /** 統一社會信用代碼 */
  unifiedSocialCreditCode?: string;

  /** 企業類型 */
  type?: EnterpriseTypeEnum;

  /** 聯系人 */
  contact?: string;

  /** 聯系電話 */
  contactPhone?: string;

  /** 郵箱 */
  email?: string;

  /** 省份代碼 */
  province?: string;

  /** 城市代碼 */
  city?: string;

  /** 區縣代碼 */
  district?: string;

  /** 詳細地址 */
  address?: string;

  /** 企業 Logo URL */
  enterpriseLogo?: string;

  /** 營業執照 URL */
  businessLicense?: string;

  /** 是否禁用 */
  disabledFlag?: boolean;
}
