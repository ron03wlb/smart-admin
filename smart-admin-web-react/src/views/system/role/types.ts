/**
 * Role Management Type Definitions
 * 角色管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import type { MenuVO } from '@/views/system/menu/types';

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

// ==================== 角色-菜單權限類型 ====================

/**
 * 角色已選菜單 VO
 */
export interface RoleMenuSelectedVO {
  /** 菜單樹列表 */
  menuTreeList: MenuVO[];

  /** 已選菜單ID列表 */
  selectedMenuId: number[];
}

/**
 * 角色菜單更新表單
 */
export interface RoleMenuUpdateForm {
  /** 角色ID */
  roleId: number;

  /** 菜單ID列表 */
  menuIdList: number[];
}

// ==================== 角色-員工管理類型 ====================

/**
 * 角色員工查詢表單
 */
export interface RoleEmployeeQueryForm {
  /** 角色ID */
  roleId: number;

  /** 關鍵字（姓名/手機號/登錄賬號） */
  keywords?: string;

  /** 頁碼 */
  pageNum?: number;

  /** 每頁數量 */
  pageSize?: number;
}

/**
 * 角色員工 VO
 */
export interface RoleEmployeeVO {
  /** 員工ID */
  employeeId: number;

  /** 姓名 */
  actualName: string;

  /** 手機號 */
  phone: string;

  /** 登錄賬號 */
  loginName: string;

  /** 部門名稱 */
  departmentName?: string;

  /** 禁用標識 */
  disabledFlag: boolean;

  /** 性別 */
  gender?: number;
}

/**
 * 角色員工批量操作表單
 */
export interface RoleEmployeeBatchForm {
  /** 角色ID */
  roleId: number;

  /** 員工ID列表 */
  employeeIdList: number[];
}

// ==================== 角色-數據範圍類型 ====================

/**
 * 數據範圍視圖類型
 */
export interface DataScopeViewType {
  /** 視圖類型 */
  viewType: number;

  /** 視圖類型名稱 */
  viewTypeName: string;
}

/**
 * 數據範圍 VO
 */
export interface DataScopeVO {
  /** 數據範圍類型 */
  dataScopeType: number;

  /** 數據範圍類型名稱 */
  dataScopeTypeName: string;

  /** 數據範圍類型描述 */
  dataScopeTypeDesc: string;

  /** 視圖類型 */
  viewType?: number;

  /** 可選視圖類型列表 */
  viewTypeList: DataScopeViewType[];
}

/**
 * 數據範圍項
 */
export interface DataScopeItem {
  /** 數據範圍類型 */
  dataScopeType: number;

  /** 視圖類型 */
  viewType: number;
}

/**
 * 數據範圍更新表單
 */
export interface DataScopeUpdateForm {
  /** 角色ID */
  roleId: number;

  /** 數據範圍項列表 */
  dataScopeItemList: DataScopeItem[];
}
