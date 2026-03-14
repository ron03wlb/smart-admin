/**
 * Role Management Type Definitions
 * 角色管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 角色 VO (View Object)
 * 用於列表展示
 */
export interface RoleVO {
  /** 角色ID */
  roleId: number;

  /** 角色名稱 */
  roleName: string;

  /** 角色編碼 */
  roleCode: string;

  /** 角色備註 */
  remark?: string;

  /** 創建時間 */
  createTime?: string;

  /** 更新時間 */
  updateTime?: string;
}

/**
 * 角色查詢表單
 */
export interface RoleQueryForm {
  /** 關鍵字（角色名稱/角色編碼/備註） */
  keywords?: string;
}

/**
 * 角色新增表單
 */
export interface RoleAddForm {
  /** 角色名稱 */
  roleName: string;

  /** 角色編碼 */
  roleCode: string;

  /** 角色備註 */
  remark?: string;
}

/**
 * 角色更新表單
 */
export interface RoleUpdateForm {
  /** 角色ID */
  roleId: number;

  /** 角色名稱 */
  roleName: string;

  /** 角色編碼 */
  roleCode: string;

  /** 角色備註 */
  remark?: string;
}

/**
 * 角色表單數據（用於 Modal）
 */
export interface RoleFormData {
  /** 角色ID（編輯時存在） */
  roleId?: number;

  /** 角色名稱 */
  roleName?: string;

  /** 角色編碼 */
  roleCode?: string;

  /** 角色備註 */
  remark?: string;
}
