/**
 * Menu Types
 * 菜單相關類型定義
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/system/menu-const.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

/**
 * 菜單類型枚舉
 */
export enum MenuTypeEnum {
  /** 目錄 */
  CATALOG = 1,
  /** 菜單 */
  MENU = 2,
  /** 功能點 */
  POINTS = 3,
}

/**
 * 菜單項接口
 * 對應後端 MenuVO
 */
export interface MenuItem {
  /** 菜單ID */
  menuId: number;

  /** 菜單名稱 */
  menuName: string;

  /** 菜單類型（1目錄 2菜單 3功能點） */
  menuType: MenuTypeEnum;

  /** 父菜單ID */
  parentId: number;

  /** 排序號 */
  sort: number;

  /** 路徑 */
  path?: string;

  /** 組件路徑 */
  component?: string;

  /** 權限標識 */
  perms?: string;

  /** 圖標 */
  icon?: string;

  /** 是否可見 */
  visibleFlag: boolean;

  /** 是否禁用 */
  disabledFlag: boolean;

  /** 是否緩存 */
  cacheFlag?: boolean;

  /** 子菜單列表 */
  children?: MenuItem[];

  /** Web權限標識 */
  webPerms?: string;
}

/**
 * 權限點接口
 */
export interface PermissionPoint {
  /** 菜單ID */
  menuId: number;

  /** Web權限標識 */
  webPerms: string;

  /** 菜單名稱 */
  menuName?: string;
}

/**
 * 登錄信息接口
 */
export interface LoginInfo {
  /** 員工ID */
  employeeId: string;

  /** 員工姓名 */
  employeeName: string;

  /** 登錄帳號 */
  loginName: string;

  /** 是否管理員 */
  administratorFlag: boolean;

  /** 菜單樹列表 */
  menuTreeList: MenuItem[];

  /** 權限點列表 */
  pointList: PermissionPoint[];
}
