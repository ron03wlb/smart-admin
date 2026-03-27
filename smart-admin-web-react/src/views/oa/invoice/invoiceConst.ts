/**
 * Invoice Constants
 * 發票信息常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

/**
 * Invoice Permissions
 * 發票信息權限定義
 */
export const INVOICE_PERMISSIONS = {
  QUERY: 'oa:invoice:query',
  ADD: 'oa:invoice:add',
  UPDATE: 'oa:invoice:update',
  DELETE: 'oa:invoice:delete',
} as const;

/**
 * Disabled Flag Options
 * 禁用狀態選項
 */
export const DISABLED_FLAG_OPTIONS = [
  { label: '啟用', value: false },
  { label: '禁用', value: true },
] as const;

/**
 * Invoice Form Rules
 * 發票信息表單驗證規則
 */
export const INVOICE_FORM_RULES = {
  invoiceHeads: [
    { required: true, message: '請輸入開票抬頭' },
    { max: 200, message: '開票抬頭不能超過200個字符' },
  ],
  taxpayerIdentificationNumber: [
    { required: true, message: '請輸入納稅人識別號' },
    { max: 200, message: '納稅人識別號不能超過200個字符' },
  ],
  accountNumber: [
    { required: true, message: '請輸入銀行賬戶' },
    { max: 200, message: '銀行賬戶不能超過200個字符' },
  ],
  bankName: [
    { required: true, message: '請輸入開戶行' },
    { max: 200, message: '開戶行不能超過200個字符' },
  ],
  disabledFlag: [{ required: true, message: '請選擇禁用狀態' }],
  remark: [{ max: 500, message: '備註不能超過500個字符' }],
  enterpriseId: [{ required: true, message: '請選擇企業' }],
} as const;

/**
 * Invoice Column Widths
 * 表格列寬配置
 */
export const INVOICE_COLUMN_WIDTHS = {
  INDEX: 60, // 序號
  INVOICE_HEADS: 150, // 開票抬頭
  TAXPAYER_ID: 180, // 納稅人識別號
  ACCOUNT_NUMBER: 150, // 銀行賬戶
  BANK_NAME: 150, // 開戶行
  REMARK: 200, // 備註
  ENTERPRISE: 150, // 企業名稱
  DISABLED_FLAG: 100, // 禁用狀態
  CREATE_USER: 120, // 創建人
  CREATE_TIME: 160, // 創建時間
  UPDATE_TIME: 160, // 更新時間
  ACTIONS: 150, // 操作
} as const;
