/**
 * Account Menu Configuration
 * 個人中心菜單配置
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/account/account-menu.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { lazy, ComponentType } from 'react';

/**
 * 菜單項接口
 */
export interface AccountMenuItem {
  menuId: string;
  menuName: string;
  component: ComponentType<any>;
}

/**
 * Account 菜單配置
 */
export const ACCOUNT_MENU: Record<string, AccountMenuItem> = {
  CENTER: {
    menuId: 'center',
    menuName: '個人中心',
    component: lazy(() => import('./components/Center')),
  },
  PASSWORD: {
    menuId: 'password',
    menuName: '修改密碼',
    component: lazy(() => import('./components/Password')),
  },
  MESSAGE: {
    menuId: 'message',
    menuName: '我的消息',
    component: lazy(() => import('./components/Message')),
  },
  NOTICE: {
    menuId: 'notice',
    menuName: '通知公告',
    component: lazy(() => import('./components/Notice')),
  },
  LOGIN_LOG: {
    menuId: 'login-log',
    menuName: '登錄日誌',
    component: lazy(() => import('./components/LoginLog')),
  },
  OPERATE_LOG: {
    menuId: 'operate-log',
    menuName: '操作日誌',
    component: lazy(() => import('./components/OperateLog')),
  },
  MFA: {
    menuId: 'mfa',
    menuName: '多因素認證',
    component: lazy(() => import('./components/Mfa')),
  },
};

/**
 * 獲取菜單列表
 */
export function getAccountMenuList(): AccountMenuItem[] {
  return Object.values(ACCOUNT_MENU);
}

/**
 * 根據 menuId 獲取菜單項
 */
export function getAccountMenuById(menuId: string): AccountMenuItem | undefined {
  return Object.values(ACCOUNT_MENU).find(menu => menu.menuId === menuId);
}
