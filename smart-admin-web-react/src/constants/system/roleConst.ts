/**
 * Role Management Constants
 * 角色管理常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 角色管理權限點
 */
export const ROLE_PERMISSION = {
  /** 新增權限 */
  ADD: 'system:role:add',

  /** 更新權限 */
  UPDATE: 'system:role:update',

  /** 刪除權限 */
  DELETE: 'system:role:delete',

  /** 查詢權限 */
  QUERY: 'system:role:query',

  /** 菜單權限更新 */
  MENU_UPDATE: 'system:role:menu:update',

  /** 員工查看 */
  EMPLOYEE_VIEW: 'system:role:employee:view',

  /** 員工添加 */
  EMPLOYEE_ADD: 'system:role:employee:add',

  /** 員工刪除 */
  EMPLOYEE_DELETE: 'system:role:employee:delete',

  /** 員工批量刪除 */
  EMPLOYEE_BATCH_DELETE: 'system:role:employee:batch:delete',

  /** 數據範圍更新 */
  DATA_SCOPE_UPDATE: 'system:role:dataScope:update',
} as const;

/**
 * 角色驗證規則
 */
export const ROLE_VALIDATION = {
  /** 角色名稱最大長度 */
  NAME_MAX_LENGTH: 20,

  /** 角色編碼最大長度 */
  CODE_MAX_LENGTH: 50,

  /** 備註最大長度 */
  REMARK_MAX_LENGTH: 200,
} as const;

/**
 * 角色表格列寬度配置
 */
export const ROLE_TABLE_COLUMNS_WIDTH = {
  /** 角色名稱 */
  roleName: 200,

  /** 角色編碼 */
  roleCode: 200,

  /** 備註 */
  remark: 250,

  /** 創建時間 */
  createTime: 180,

  /** 更新時間 */
  updateTime: 180,

  /** 操作列 */
  operate: 200,
} as const;
