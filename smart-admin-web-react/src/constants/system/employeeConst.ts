/**
 * Employee Management Constants
 * 員工管理常量定義
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/employee/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 員工管理權限點
 */
export const EMPLOYEE_PERMISSION = {
  /** 添加員工 */
  ADD: 'system:employee:add',
  /** 更新員工 */
  UPDATE: 'system:employee:update',
  /** 刪除員工 */
  DELETE: 'system:employee:delete',
  /** 重置密碼 */
  RESET_PASSWORD: 'system:employee:password:reset',
  /** 禁用/啟用員工 */
  DISABLED: 'system:employee:disabled',
  /** 調整部門 */
  UPDATE_DEPARTMENT: 'system:employee:department:update',
} as const;

/**
 * 驗證規則
 */
export const EMPLOYEE_VALIDATION = {
  /** 手機號正則（中國大陸） */
  PHONE_REGEX: /^1[3-9]\d{9}$/,
  /** 郵箱正則 */
  EMAIL_REGEX: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  /** 姓名最大長度 */
  NAME_MAX_LENGTH: 30,
  /** 登錄名最大長度 */
  LOGIN_NAME_MAX_LENGTH: 30,
} as const;

/**
 * 性別標籤映射
 */
export const GENDER_LABELS = {
  1: '男',
  2: '女',
} as const;

/**
 * 員工狀態標籤映射
 */
export const EMPLOYEE_STATUS_LABELS = {
  0: '啟用',
  1: '禁用',
} as const;

/**
 * 離職狀態標籤映射
 */
export const LEAVE_STATUS_LABELS = {
  0: '在職',
  1: '離職',
} as const;

/**
 * 表格列寬度配置
 */
export const EMPLOYEE_TABLE_COLUMNS_WIDTH = {
  actualName: 85,
  gender: 70,
  loginName: 100,
  phone: 85,
  email: 100,
  administratorFlag: 60,
  disabledFlag: 60,
  positionName: 100,
  roleNameList: 100,
  departmentName: 200,
  operate: 140,
} as const;
