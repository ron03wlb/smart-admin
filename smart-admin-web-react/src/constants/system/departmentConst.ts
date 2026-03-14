/**
 * Department Management Constants
 * 部門管理常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 部門管理權限點
 */
export const DEPARTMENT_PERMISSION = {
  /** 查詢權限 */
  QUERY: 'support:department:query',

  /** 新增權限 */
  ADD: 'system:department:add',

  /** 更新權限 */
  UPDATE: 'system:department:update',

  /** 刪除權限 */
  DELETE: 'system:department:delete',
} as const;

/**
 * 部門驗證規則
 */
export const DEPARTMENT_VALIDATION = {
  /** 部門名稱最大長度 */
  NAME_MAX_LENGTH: 50,
} as const;

/**
 * 部門表格列寬度配置
 */
export const DEPARTMENT_TABLE_COLUMNS_WIDTH = {
  /** 部門名稱 */
  departmentName: 300,

  /** 負責人 */
  managerName: 120,

  /** 排序 */
  sort: 100,

  /** 創建時間 */
  createTime: 180,

  /** 更新時間 */
  updateTime: 180,

  /** 操作列 */
  operate: 240,
} as const;

/**
 * 部門常量
 */
export const DEPARTMENT_CONSTANTS = {
  /** 頂級部門的 parentId */
  TOP_PARENT_ID: 0,
} as const;
