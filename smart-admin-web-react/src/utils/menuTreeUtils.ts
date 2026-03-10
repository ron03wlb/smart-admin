/**
 * Menu Tree Utilities
 * 菜單樹構建工具函數
 *
 * 參考：Vue 版本 smart-admin-web/src/store/modules/system/user.ts (lines 313-364)
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { MenuItem, MenuTypeEnum, PermissionPoint } from '@/types/menu';

/**
 * 構建菜單樹
 * 參考：Vue 版本 buildMenuTree() - line 343-353
 *
 * @param menuList 扁平菜單列表
 * @returns 樹形菜單結構
 */
export function buildMenuTree(menuList: MenuItem[]): MenuItem[] {
  if (!menuList || menuList.length === 0) {
    return [];
  }

  // 過濾出頂級菜單（parentId 為 0 或 null）
  const rootMenus = menuList.filter(menu => !menu.parentId || menu.parentId === 0);

  // 構建每個頂級菜單的子樹
  const menuTree = rootMenus.map(menu => buildMenuChildren(menu, menuList));

  // 按 sort 排序
  return menuTree.sort((a, b) => a.sort - b.sort);
}

/**
 * 遞歸構建子菜單
 * 參考：Vue 版本 buildMenuChildren() - line 355-364
 *
 * @param parentMenu 父菜單
 * @param allMenus 所有菜單列表
 * @returns 帶子菜單的菜單項
 */
export function buildMenuChildren(parentMenu: MenuItem, allMenus: MenuItem[]): MenuItem {
  const menu = { ...parentMenu };

  // 找到所有子菜單
  const children = allMenus.filter(item => item.parentId === menu.menuId);

  if (children.length > 0) {
    // 遞歸構建子菜單並排序
    menu.children = children
      .map(child => buildMenuChildren(child, allMenus))
      .sort((a, b) => a.sort - b.sort);
  }

  return menu;
}

/**
 * 構建菜單父ID映射表
 * 參考：Vue 版本 buildMenuParentIdListMap() - line 313-335
 *
 * 用途：用於麵包屑導航、菜單高亮等場景
 * 格式：{ menuId: [parentId1, parentId2, ..., menuId] }
 *
 * @param menuTree 菜單樹
 * @returns 父ID映射表
 */
export function buildMenuParentIdListMap(menuTree: MenuItem[]): Record<number, number[]> {
  const map: Record<number, number[]> = {};

  const traverse = (menus: MenuItem[], parentIds: number[] = []) => {
    menus.forEach(menu => {
      // 當前菜單的所有父ID + 自身ID
      const currentPath = [...parentIds, menu.menuId];
      map[menu.menuId] = currentPath;

      // 遞歸處理子菜單
      if (menu.children && menu.children.length > 0) {
        traverse(menu.children, currentPath);
      }
    });
  };

  traverse(menuTree);
  return map;
}

/**
 * 過濾菜單樹（僅保留目錄和菜單，排除功能點）
 * 用於側邊欄菜單顯示
 *
 * @param menuTree 完整菜單樹
 * @returns 過濾後的菜單樹（不含功能點）
 */
export function filterMenuTreeForDisplay(menuTree: MenuItem[]): MenuItem[] {
  return menuTree
    .filter(menu => menu.menuType !== MenuTypeEnum.POINTS && menu.visibleFlag)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenuTreeForDisplay(menu.children) : undefined,
    }));
}

/**
 * 提取所有權限點（menuType = 3）
 * 用於權限檢查
 *
 * @param menuList 扁平菜單列表
 * @returns 權限點列表
 */
export function extractPermissionPoints(menuList: MenuItem[]): PermissionPoint[] {
  return menuList
    .filter(menu => menu.menuType === MenuTypeEnum.POINTS && menu.webPerms)
    .map(menu => ({
      menuId: menu.menuId,
      webPerms: menu.webPerms!,
      menuName: menu.menuName,
    }));
}

/**
 * 生成路由列表（僅包含菜單類型）
 * 用於動態路由配置
 *
 * @param menuTree 菜單樹
 * @returns 路由路徑列表
 */
export function generateRouteList(menuTree: MenuItem[]): string[] {
  const routes: string[] = [];

  const traverse = (menus: MenuItem[]) => {
    menus.forEach(menu => {
      // 只添加菜單類型且有路徑的項
      if (menu.menuType === MenuTypeEnum.MENU && menu.path) {
        routes.push(menu.path);
      }

      // 遞歸處理子菜單
      if (menu.children && menu.children.length > 0) {
        traverse(menu.children);
      }
    });
  };

  traverse(menuTree);
  return routes;
}
