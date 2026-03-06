/**
 * useBreadcrumb Hook
 *
 * 功能：
 * 1. 根據當前路由自動生成麵包屑數據
 * 2. 提供點擊導航功能
 * 3. 處理邊界情況（無匹配菜單時僅顯示「首頁」）
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */
import { useMemo, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { MenuItem } from '@/types/user.types';
import { selectMenuTree } from '@/store/slices/userSlice';
import { findMenuIdByPath, getParentKeys } from './useMenu';

/**
 * 麵包屑項類型
 */
export interface BreadcrumbItem {
  /**
   * 麵包屑項標題
   */
  title: string;

  /**
   * 麵包屑項路徑（用於導航）
   */
  path?: string;

  /**
   * 是否為首頁（用於在組件中渲染圖標）
   */
  isHome?: boolean;
}

/**
 * useBreadcrumb Hook 返回值
 */
export interface UseBreadcrumbReturn {
  /**
   * 麵包屑項列表
   */
  breadcrumbItems: BreadcrumbItem[];

  /**
   * 處理麵包屑項點擊
   */
  handleBreadcrumbClick: (path: string) => void;
}

/**
 * 根據菜單 ID 查找菜單項
 */
function findMenuById(menuList: MenuItem[], menuId: string): MenuItem | null {
  for (const menu of menuList) {
    if (menu.menuId === menuId) {
      return menu;
    }
    if (menu.children && menu.children.length > 0) {
      const found = findMenuById(menu.children, menuId);
      if (found) return found;
    }
  }
  return null;
}

/**
 * useBreadcrumb Hook
 */
export function useBreadcrumb(): UseBreadcrumbReturn {
  const navigate = useNavigate();
  const location = useLocation();
  const menuTree = useSelector(selectMenuTree);

  /**
   * 生成麵包屑項列表
   */
  const breadcrumbItems = useMemo((): BreadcrumbItem[] => {
    // 1. 始終包含「首頁」作為第一項
    const items: BreadcrumbItem[] = [
      {
        title: '首頁',
        path: '/home',
        isHome: true,
      },
    ];

    // 2. 如果當前路由是首頁，直接返回
    if (location.pathname === '/' || location.pathname === '/home') {
      return items;
    }

    // 3. 根據當前路由查找對應的菜單 ID
    const currentMenuId = findMenuIdByPath(menuTree, location.pathname);

    // 4. 如果找不到匹配的菜單，僅顯示「首頁」（優雅降級）
    if (!currentMenuId) {
      return items;
    }

    // 5. 獲取父菜單 ID 列表（從根到當前菜單的路徑）
    const parentKeys = getParentKeys(menuTree, currentMenuId);

    // 6. 為每個父菜單生成麵包屑項
    parentKeys.forEach((parentId) => {
      const parentMenu = findMenuById(menuTree, parentId);
      if (parentMenu) {
        items.push({
          title: parentMenu.menuName,
          path: parentMenu.path || undefined,
        });
      }
    });

    // 7. 添加當前菜單項（不可點擊，path 設為 undefined）
    const currentMenu = findMenuById(menuTree, currentMenuId);
    if (currentMenu) {
      items.push({
        title: currentMenu.menuName,
        path: undefined, // 最後一項不可點擊
      });
    }

    return items;
  }, [location.pathname, menuTree]);

  /**
   * 處理麵包屑項點擊
   */
  const handleBreadcrumbClick = useCallback(
    (path: string) => {
      navigate(path);
    },
    [navigate]
  );

  return {
    breadcrumbItems,
    handleBreadcrumbClick,
  };
}
