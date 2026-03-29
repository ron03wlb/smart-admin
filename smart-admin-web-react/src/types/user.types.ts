/**
 * 用戶相關類型定義
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */

import type { MenuItem } from '@/types/menu';
export type { MenuItem } from '@/types/menu';

/**
 * 菜單功能點（權限點）
 *
 * 對應後端 MenuVO 中 menuType 為 POINTS 的記錄
 */
export interface MenuPoint {
  /** 菜單 ID */
  menuId: string;

  /** 菜單名稱 */
  menuName: string;

  /** 前端權限標識（用於權限檢查，對應 v-privilege 指令的值） */
  webPerms: string;

  /** 後端權限標識（用於後端 @SaCheckPermission） */
  apiPerms?: string;

  /** 權限類型 */
  permsType?: number;

  /** 是否可見 */
  visibleFlag: boolean;

  /** 是否禁用 */
  disabledFlag: boolean;

  /** 關聯菜單 ID */
  contextMenuId?: string;
}

/**
 * 用戶狀態（Redux State）
 */
export interface UserState {
  /** 用戶 Token */
  token: string;

  /** 員工 ID */
  employeeId: string;

  /** 員工名稱 */
  employeeName: string;

  /** 是否為超級管理員 */
  administratorFlag: boolean;

  /** 功能點權限列表（對應 Vue pointsList） */
  pointsList: MenuPoint[];

  /** 菜單樹（側邊欄顯示用，已構建父子結構） */
  menuTree: MenuItem[];

  /** 有路由的菜單列表（動態路由生成用，扁平結構） */
  menuRouterList: MenuItem[];

  /** 部門 ID */
  departmentId?: string;

  /** 部門名稱 */
  departmentName?: string;
}

/**
 * 登錄結果（API 響應）
 */
export interface LoginResult {
  /** Token */
  token: string;

  /** 員工 ID */
  employeeId: string;

  /** 員工名稱 */
  employeeName: string;

  /** 是否為超級管理員 */
  administratorFlag: boolean;

  /** 完整菜單列表（包含目錄、菜單、功能點） */
  menuList: MenuItem[];

  /** 租戶 ID（多租戶場景） */
  tenantId?: string;

  /** 時區（多租戶場景，例如 'Asia/Taipei'） */
  timezone?: string;

  /** 部門 ID */
  departmentId?: string;

  /** 部門名稱 */
  departmentName?: string;
}
