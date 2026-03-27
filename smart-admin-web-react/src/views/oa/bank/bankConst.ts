/**
 * Bank Constants
 * 銀行信息常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

/**
 * Bank Permissions
 * 銀行信息權限定義
 */
export const BANK_PERMISSIONS = {
  QUERY: 'oa:bank:query',
  ADD: 'oa:bank:add',
  UPDATE: 'oa:bank:update',
  DELETE: 'oa:bank:delete',
} as const;

/**
 * Business Flag Options
 * 是否對公選項
 */
export const BUSINESS_FLAG_OPTIONS = [
  { label: '對公', value: true },
  { label: '對私', value: false },
] as const;

/**
 * Disabled Flag Options
 * 禁用狀態選項
 */
export const DISABLED_FLAG_OPTIONS = [
  { label: '啟用', value: false },
  { label: '禁用', value: true },
] as const;

/**
 * Bank Form Rules
 * 銀行信息表單驗證規則
 */
export const BANK_FORM_RULES = {
  bankName: [
    { required: true, message: '請輸入開戶銀行' },
    { max: 200, message: '開戶銀行不能超過200個字符' },
  ],
  accountName: [
    { required: true, message: '請輸入賬戶名稱' },
    { max: 200, message: '賬戶名稱不能超過200個字符' },
  ],
  accountNumber: [
    { required: true, message: '請輸入賬號' },
    { max: 200, message: '賬號不能超過200個字符' },
  ],
  remark: [{ max: 500, message: '備註不能超過500個字符' }],
  businessFlag: [{ required: true, message: '請選擇是否對公' }],
  enterpriseId: [{ required: true, message: '請選擇企業' }],
  disabledFlag: [{ required: true, message: '請選擇禁用狀態' }],
} as const;

/**
 * Bank Column Widths
 * 表格列寬配置
 */
export const BANK_COLUMN_WIDTHS = {
  INDEX: 60, // 序號
  BANK_NAME: 150, // 開戶銀行
  ACCOUNT_NAME: 150, // 賬戶名稱
  ACCOUNT_NUMBER: 180, // 賬號
  REMARK: 200, // 備註
  BUSINESS_FLAG: 100, // 是否對公
  ENTERPRISE: 150, // 企業名稱
  DISABLED_FLAG: 100, // 禁用狀態
  CREATE_USER: 120, // 創建人
  CREATE_TIME: 160, // 創建時間
  UPDATE_TIME: 160, // 更新時間
  ACTIONS: 150, // 操作
} as const;
