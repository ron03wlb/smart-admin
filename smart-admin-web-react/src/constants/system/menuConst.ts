/**
 * Menu Management Constants
 * 菜單管理常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { MenuTypeEnum, PermsTypeEnum } from '@/views/system/menu/types';

/**
 * 菜單管理權限點
 */
export const MENU_PERMISSION = {
  /** 新增權限 */
  ADD: 'system:menu:add',

  /** 更新權限 */
  UPDATE: 'system:menu:update',

  /** 批量刪除權限 */
  BATCH_DELETE: 'system:menu:batchDelete',
} as const;

/**
 * 菜單驗證規則
 */
export const MENU_VALIDATION = {
  /** 菜單名稱最大長度 */
  NAME_MAX_LENGTH: 20,

  /** 路由地址最大長度 */
  PATH_MAX_LENGTH: 100,

  /** 外鏈地址最大長度 */
  FRAME_URL_MAX_LENGTH: 500,
} as const;

/**
 * 菜單類型標籤
 */
export const MENU_TYPE_LABELS = {
  [MenuTypeEnum.CATALOG]: '目錄',
  [MenuTypeEnum.MENU]: '菜單',
  [MenuTypeEnum.POINTS]: '功能點',
} as const;

/**
 * 菜單類型顏色
 */
export const MENU_TYPE_COLORS = {
  [MenuTypeEnum.CATALOG]: 'red',
  [MenuTypeEnum.MENU]: 'blue',
  [MenuTypeEnum.POINTS]: 'orange',
} as const;

/**
 * 權限類型標籤
 */
export const PERMS_TYPE_LABELS = {
  [PermsTypeEnum.SA_TOKEN]: 'Sa-Token',
  [PermsTypeEnum.SPRING_SECURITY]: 'Spring Security',
} as const;

/**
 * 菜單表格列寬度配置
 */
export const MENU_TABLE_COLUMNS_WIDTH = {
  /** 菜單名稱 */
  menuName: 200,

  /** 菜單類型 */
  menuType: 90,

  /** 菜單圖標 */
  icon: 80,

  /** 路由地址 */
  path: 150,

  /** 組件/外鏈 */
  component: 250,

  /** 外鏈 */
  frameFlag: 70,

  /** 權限類型 */
  permsType: 130,

  /** 前端權限 */
  webPerms: 180,

  /** 後端權限 */
  apiPerms: 180,

  /** 緩存 */
  cacheFlag: 70,

  /** 顯示 */
  visibleFlag: 70,

  /** 禁用 */
  disabledFlag: 70,

  /** 排序 */
  sort: 70,

  /** 操作列 */
  operate: 200,
} as const;

/**
 * 菜單常量
 */
export const MENU_CONSTANTS = {
  /** 頂級菜單的 parentId */
  TOP_PARENT_ID: 0,
} as const;

/**
 * Flag 標籤映射
 */
export const FLAG_LABELS = {
  true: '是',
  false: '否',
} as const;
