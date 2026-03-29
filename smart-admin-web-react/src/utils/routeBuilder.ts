/**
 * 動態路由構建工具
 *
 * 將後端菜單列表轉換為 React Router 路由配置
 * 對應 Vue 的 buildRoutes() 函數
 */
import React from 'react';
import type { RouteObject } from 'react-router-dom';
import type { MenuItem } from '@/types/user.types';

/**
 * 路由元數據（存儲在 route.handle 中）
 */
export interface RouteMeta {
  menuId: string;
  title: string;
  icon?: string;
  keepAlive?: boolean;
  frameFlag?: boolean;
  frameUrl?: string;
}

// Vite glob 導入所有 views 下的 .tsx 組件
const modules = import.meta.glob('../views/**/*.tsx') as Record<
  string,
  () => Promise<{ default: React.ComponentType }>
>;

/**
 * 未遷移頁面的佔位組件
 */
const NotMigrated = React.lazy(() => import('@/components/framework/NotMigrated'));

/**
 * IFrame 外鏈組件
 */
const IFramePage = React.lazy(() => import('@/components/framework/IFramePage'));

/**
 * 根據後端 component 路徑找到對應的 React 組件
 *
 * 後端路徑格式: /system/employee/employee-list.vue
 * 需要匹配: ../views/system/employee/EmployeeList.tsx
 */
function resolveComponent(componentPath: string): React.LazyExoticComponent<React.ComponentType> {
  if (!componentPath) {
    return NotMigrated;
  }

  // 標準化路徑: 確保以 / 開頭
  const normalized = componentPath.startsWith('/') ? componentPath : `/${componentPath}`;

  // 嘗試直接匹配（替換 .vue → .tsx）
  const tsxPath = `../views${normalized.replace(/\.vue$/, '.tsx')}`;
  if (modules[tsxPath]) {
    return React.lazy(modules[tsxPath]);
  }

  // 嘗試 kebab-case → PascalCase 轉換
  const parts = normalized.split('/');
  const fileName = parts[parts.length - 1].replace(/\.vue$/, '');
  const pascalName = fileName
    .split('-')
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
    .join('');
  const pascalPath = `../views${parts.slice(0, -1).join('/')}/${pascalName}.tsx`;
  if (modules[pascalPath]) {
    return React.lazy(modules[pascalPath]);
  }

  // 嘗試查找 index.tsx
  const indexPath = `../views${parts.slice(0, -1).join('/')}/index.tsx`;
  if (modules[indexPath]) {
    return React.lazy(modules[indexPath]);
  }

  // 找不到匹配的組件，使用佔位頁面
  return NotMigrated;
}

/**
 * 將菜單列表轉換為 React Router 路由配置
 *
 * 對應 Vue 的 buildRoutes() 函數
 */
export function buildDynamicRoutes(menuRouterList: MenuItem[]): RouteObject[] {
  return menuRouterList
    .filter((menu) => {
      if (!menu.menuId) return false;
      if (!menu.path) return false;
      if (menu.deletedFlag) return false;
      return true;
    })
    .map((menu) => {
      const path = menu.path!.startsWith('/') ? menu.path!.slice(1) : menu.path!;

      const Component = menu.frameFlag
        ? IFramePage
        : resolveComponent(menu.component || '');

      return {
        path,
        element: React.createElement(
          React.Suspense,
          { fallback: React.createElement('div', { style: { padding: 24, textAlign: 'center' as const } }, 'Loading...') },
          React.createElement(Component, menu.frameFlag ? { url: menu.frameUrl } : null)
        ),
        handle: {
          menuId: menu.menuId.toString(),
          title: menu.menuName,
          icon: menu.icon,
          keepAlive: menu.cacheFlag,
          frameFlag: menu.frameFlag,
          frameUrl: menu.frameUrl,
        } satisfies RouteMeta,
      };
    });
}

/**
 * 構建菜單樹（從扁平列表構建父子結構）
 *
 * 對應 Vue 的 buildMenuTree() 函數
 */
export function buildMenuTree(menuList: MenuItem[]): MenuItem[] {
  // 過濾有效菜單（目錄和菜單，排除功能點）
  const catalogAndMenuList = menuList.filter(
    (menu) =>
      menu.menuType !== 'POINTS' &&
      menu.visibleFlag &&
      !menu.disabledFlag
  );

  // 獲取頂級目錄（parentId === '0' 或 parentId === 0）
  const topList = catalogAndMenuList.filter(
    (menu) => !menu.parentId || menu.parentId === '0' || menu.parentId === 0 as unknown as string
  );

  // 遞迴構建子菜單
  function buildChildren(parent: MenuItem): void {
    const children = catalogAndMenuList.filter(
      (m) => String(m.parentId) === String(parent.menuId)
    );
    if (children.length === 0) return;

    parent.children = children.sort((a, b) => (a.sort || 0) - (b.sort || 0));
    for (const child of children) {
      buildChildren(child);
    }
  }

  for (const top of topList) {
    buildChildren(top);
  }

  return topList.sort((a, b) => (a.sort || 0) - (b.sort || 0));
}

/**
 * 構建父級菜單映射（用於麵包屑和菜單高亮）
 *
 * 返回 Map<menuId, parentChain[]>
 */
export function buildMenuParentMap(
  menuTree: MenuItem[]
): Record<string, Array<{ id: string; title: string }>> {
  const result: Record<string, Array<{ id: string; title: string }>> = {};

  function walk(list: MenuItem[], parents: Array<{ id: string; title: string }>): void {
    for (const menu of list) {
      result[String(menu.menuId)] = [...parents];
      if (menu.children && menu.children.length > 0) {
        walk(menu.children, [...parents, { id: String(menu.menuId), title: menu.menuName }]);
      }
    }
  }

  walk(menuTree, []);
  return result;
}
