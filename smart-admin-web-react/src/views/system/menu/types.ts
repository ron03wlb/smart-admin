/**
 * Menu Management Type Definitions
 * 菜單管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 菜單類型枚舉
 */
export const MenuTypeEnum = {
  /** 目錄 */
  CATALOG: 1,
  /** 菜單 */
  MENU: 2,
  /** 功能點/權限點 */
  POINTS: 3,
} as const;
export type MenuTypeEnum = (typeof MenuTypeEnum)[keyof typeof MenuTypeEnum];

/**
 * 權限類型枚舉
 */
export const PermsTypeEnum = {
  /** Sa-Token */
  SA_TOKEN: 1,
  /** Spring Security */
  SPRING_SECURITY: 2,
} as const;
export type PermsTypeEnum = (typeof PermsTypeEnum)[keyof typeof PermsTypeEnum];

/**
 * 菜單 VO（View Object）
 * 用於列表展示和樹形結構
 */
export interface MenuVO {
  /** 菜單ID */
  menuId: number;

  /** 菜單名稱 */
  menuName: string;

  /** 菜單類型（1=目錄，2=菜單，3=功能點） */
  menuType: MenuTypeEnum;

  /** 上級菜單ID（0 表示頂級） */
  parentId: number;

  /** 菜單圖標 */
  icon?: string;

  /** 路由地址（僅菜單類型有） */
  path?: string;

  /** 組件地址（僅菜單類型有，非外鏈） */
  component?: string;

  /** 是否外鏈 */
  frameFlag?: boolean;

  /** 外鏈地址（僅外鏈時有） */
  frameUrl?: string;

  /** 是否緩存 */
  cacheFlag?: boolean;

  /** 是否顯示 */
  visibleFlag?: boolean;

  /** 是否禁用 */
  disabledFlag?: boolean;

  /** 排序值（值越小越靠前） */
  sort: number;

  /** 權限類型（功能點用） */
  permsType?: PermsTypeEnum;

  /** 前端權限字符串（功能點用） */
  webPerms?: string;

  /** 後端權限字符串（功能點用） */
  apiPerms?: string;

  /** 功能點關聯菜單ID */
  contextMenuId?: number;

  /** 創建時間 */
  createTime?: string;

  /** 更新時間 */
  updateTime?: string;

  /** 子菜單列表（樹形結構） */
  children?: MenuVO[];
}

/**
 * 菜單查詢表單
 */
export interface MenuQueryForm {
  /** 關鍵字（菜單名稱/路由地址/組件路徑/權限字符串） */
  keywords?: string;

  /** 菜單類型 */
  menuType?: MenuTypeEnum;

  /** 是否外鏈 */
  frameFlag?: boolean;

  /** 是否緩存 */
  cacheFlag?: boolean;

  /** 是否顯示 */
  visibleFlag?: boolean;

  /** 是否禁用 */
  disabledFlag?: boolean;
}

/**
 * 菜單新增表單
 */
export interface MenuAddForm {
  /** 菜單名稱 */
  menuName: string;

  /** 菜單類型 */
  menuType: MenuTypeEnum;

  /** 上級菜單ID */
  parentId: number;

  /** 菜單圖標 */
  icon?: string;

  /** 路由地址 */
  path?: string;

  /** 組件地址 */
  component?: string;

  /** 是否外鏈 */
  frameFlag?: boolean;

  /** 外鏈地址 */
  frameUrl?: string;

  /** 是否緩存 */
  cacheFlag?: boolean;

  /** 是否顯示 */
  visibleFlag?: boolean;

  /** 是否禁用 */
  disabledFlag?: boolean;

  /** 排序值 */
  sort?: number;

  /** 權限類型 */
  permsType?: PermsTypeEnum;

  /** 前端權限 */
  webPerms?: string;

  /** 後端權限 */
  apiPerms?: string;

  /** 功能點關聯菜單ID */
  contextMenuId?: number;
}

/**
 * 菜單更新表單
 */
export interface MenuUpdateForm {
  /** 菜單ID */
  menuId: number;

  /** 菜單名稱 */
  menuName: string;

  /** 菜單類型 */
  menuType: MenuTypeEnum;

  /** 上級菜單ID */
  parentId: number;

  /** 菜單圖標 */
  icon?: string;

  /** 路由地址 */
  path?: string;

  /** 組件地址 */
  component?: string;

  /** 是否外鏈 */
  frameFlag?: boolean;

  /** 外鏈地址 */
  frameUrl?: string;

  /** 是否緩存 */
  cacheFlag?: boolean;

  /** 是否顯示 */
  visibleFlag?: boolean;

  /** 是否禁用 */
  disabledFlag?: boolean;

  /** 排序值 */
  sort?: number;

  /** 權限類型 */
  permsType?: PermsTypeEnum;

  /** 前端權限 */
  webPerms?: string;

  /** 後端權限 */
  apiPerms?: string;

  /** 功能點關聯菜單ID */
  contextMenuId?: number;
}

/**
 * 菜單表單數據（用於 Modal）
 */
export interface MenuFormData {
  /** 菜單ID（編輯時存在） */
  menuId?: number;

  /** 菜單名稱 */
  menuName?: string;

  /** 菜單類型 */
  menuType?: MenuTypeEnum;

  /** 上級菜單ID */
  parentId?: number;

  /** 菜單圖標 */
  icon?: string;

  /** 路由地址 */
  path?: string;

  /** 組件地址 */
  component?: string;

  /** 是否外鏈 */
  frameFlag?: boolean;

  /** 外鏈地址 */
  frameUrl?: string;

  /** 是否緩存 */
  cacheFlag?: boolean;

  /** 是否顯示 */
  visibleFlag?: boolean;

  /** 是否禁用 */
  disabledFlag?: boolean;

  /** 排序值 */
  sort?: number;

  /** 權限類型 */
  permsType?: PermsTypeEnum;

  /** 前端權限 */
  webPerms?: string;

  /** 後端權限 */
  apiPerms?: string;

  /** 功能點關聯菜單ID */
  contextMenuId?: number;
}

/**
 * 菜單樹節點（用於 TreeSelect）
 */
export interface MenuTreeNode {
  /** 節點值（菜單ID） */
  value: number;

  /** 節點標題（菜單名稱） */
  title: string;

  /** 菜單類型 */
  menuType?: MenuTypeEnum;

  /** 子節點 */
  children?: MenuTreeNode[];
}
