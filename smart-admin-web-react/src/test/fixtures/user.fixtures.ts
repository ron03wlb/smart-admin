/**
 * 用戶相關測試數據生成器（Fixtures）
 *
 * 功能：
 * 1. createMockPermissions() - 生成 MenuPoint[] 數據
 * 2. createMockMenuTree() - 生成 MenuItem[] 數據
 * 3. createMockLoginResult() - 生成 LoginResult 數據
 *
 * 使用場景：
 * - 單元測試：需要 Mock 用戶數據
 * - 集成測試：需要預設權限和菜單數據
 * - 避免在測試文件中 inline 定義重複的 Mock 數據
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import type { MenuPoint, MenuItem, LoginResult } from '@/types/user.types';

/**
 * 創建 Mock 權限列表
 *
 * @param permissionCodes - 權限編碼列表
 * @returns MenuPoint[] 數據
 *
 * @example
 * ```typescript
 * // 創建單個權限
 * const permissions = createMockPermissions(['system:user:add']);
 *
 * // 創建多個權限
 * const permissions = createMockPermissions([
 *   'system:user:add',
 *   'system:user:edit',
 *   'system:user:delete',
 * ]);
 * ```
 */
export function createMockPermissions(permissionCodes: string[]): MenuPoint[] {
  return permissionCodes.map((code, index) => ({
    menuId: `${index + 1}`,
    menuName: `權限 ${code}`,
    menuType: 'POINTS' as const,
    webPerms: code,
    visibleFlag: true,
    disabledFlag: false,
    parentId: '0',
    path: '',
    component: '',
    icon: '',
    sort: index,
    permsType: 1,
    apiPerms: '',
    frameSrc: '',
    frameFlag: false,
    cacheFlag: false,
    contextMenuId: '',
  }));
}

/**
 * 創建 Mock 菜單樹
 *
 * @param menuItems - 自定義菜單項（可選）
 * @returns MenuItem[] 數據
 *
 * @example
 * ```typescript
 * // 使用默認菜單樹
 * const menuTree = createMockMenuTree();
 *
 * // 自定義菜單樹
 * const customMenuTree = createMockMenuTree([
 *   { menuId: '1', menuName: '玩家管理', menuType: 'CATALOG' },
 *   { menuId: '2', menuName: '玩家列表', menuType: 'MENU', parentId: '1', path: '/player/list' },
 *   { menuId: '3', menuName: '錢包管理', menuType: 'CATALOG' },
 *   { menuId: '4', menuName: '錢包概覽', menuType: 'MENU', parentId: '3', path: '/wallet/overview' },
 * ]);
 * ```
 */
export function createMockMenuTree(
  menuItems?: Partial<MenuItem>[]
): MenuItem[] {
  if (menuItems) {
    return menuItems.map((item, index) => ({
      menuId: item.menuId || `${index + 1}`,
      menuName: item.menuName || `菜單項 ${index + 1}`,
      menuType: item.menuType || 'MENU',
      webPerms: item.webPerms || '',
      visibleFlag: item.visibleFlag !== undefined ? item.visibleFlag : true,
      disabledFlag: item.disabledFlag !== undefined ? item.disabledFlag : false,
      parentId: item.parentId || '0',
      path: item.path || `/path/${index + 1}`,
      component: item.component || '',
      icon: item.icon || '',
      sort: item.sort !== undefined ? item.sort : index,
      permsType: item.permsType || 1,
      apiPerms: item.apiPerms || '',
      frameSrc: item.frameSrc || '',
      frameFlag: item.frameFlag || false,
      cacheFlag: item.cacheFlag || false,
      contextMenuId: item.contextMenuId || '',
    }));
  }

  // 默認菜單樹（模擬 iGaming 平台結構）
  return [
    {
      menuId: '1',
      menuName: '玩家管理',
      menuType: 'CATALOG' as const,
      webPerms: '',
      visibleFlag: true,
      disabledFlag: false,
      parentId: '0',
      path: '/player',
      component: '',
      icon: 'UserOutlined',
      sort: 1,
      permsType: 1,
      apiPerms: '',
      frameSrc: '',
      frameFlag: false,
      cacheFlag: false,
      contextMenuId: '',
    },
    {
      menuId: '2',
      menuName: '玩家列表',
      menuType: 'MENU' as const,
      webPerms: 'player:list',
      visibleFlag: true,
      disabledFlag: false,
      parentId: '1',
      path: '/player/list',
      component: 'PlayerList',
      icon: '',
      sort: 1,
      permsType: 1,
      apiPerms: '',
      frameSrc: '',
      frameFlag: false,
      cacheFlag: false,
      contextMenuId: '',
    },
    {
      menuId: '3',
      menuName: '錢包管理',
      menuType: 'CATALOG' as const,
      webPerms: '',
      visibleFlag: true,
      disabledFlag: false,
      parentId: '0',
      path: '/wallet',
      component: '',
      icon: 'WalletOutlined',
      sort: 2,
      permsType: 1,
      apiPerms: '',
      frameSrc: '',
      frameFlag: false,
      cacheFlag: false,
      contextMenuId: '',
    },
    {
      menuId: '4',
      menuName: '錢包概覽',
      menuType: 'MENU' as const,
      webPerms: 'wallet:overview',
      visibleFlag: true,
      disabledFlag: false,
      parentId: '3',
      path: '/wallet/overview',
      component: 'WalletOverview',
      icon: '',
      sort: 1,
      permsType: 1,
      apiPerms: '',
      frameSrc: '',
      frameFlag: false,
      cacheFlag: false,
      contextMenuId: '',
    },
  ];
}

/**
 * 創建 Mock LoginResult
 *
 * @param overrides - 可選覆寫字段
 * @returns LoginResult 數據
 *
 * @example
 * ```typescript
 * // 創建默認登錄結果
 * const loginResult = createMockLoginResult();
 *
 * // 創建超級管理員登錄結果
 * const adminLoginResult = createMockLoginResult({
 *   administratorFlag: true,
 *   employeeName: 'Admin User',
 * });
 *
 * // 創建有自定義權限的登錄結果
 * const loginResult = createMockLoginResult({
 *   menuList: [
 *     ...createMockMenuTree(),
 *     ...createMockPermissions(['system:user:add', 'system:user:edit']),
 *   ],
 * });
 * ```
 */
export function createMockLoginResult(
  overrides?: Partial<LoginResult>
): LoginResult {
  return {
    token: 'mock-token-123456',
    employeeId: 'mock-employee-id',
    employeeName: 'Mock User',
    administratorFlag: false,
    menuList: createMockMenuTree(),
    ...overrides,
  };
}
