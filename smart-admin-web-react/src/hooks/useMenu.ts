/**
 * useMenu Hook
 *
 * 功能：
 * 1. 處理菜單樹渲染邏輯
 * 2. 管理菜單展開/折疊狀態
 * 3. 處理菜單項點擊導航
 * 4. 根據當前路由自動選中菜單項
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */
import { useMemo, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import type { MenuProps } from 'antd';
import type { MenuItem } from '@/types/user.types';
import { selectMenuTree } from '@/store/slices/userSlice';
import {
  selectCollapsed,
  selectOpenKeys,
  selectSelectedMenuId,
  toggleCollapsed,
  setOpenKeys,
  setSelectedMenuId,
} from '@/store/slices/menuSlice';

/**
 * 菜單項類型（Ant Design Menu 使用）
 */
export type MenuItemType = Required<MenuProps>['items'][number];

/**
 * useMenu Hook 返回值
 */
export interface UseMenuReturn {
  /**
   * 處理後的菜單項列表（Ant Design Menu 格式）
   */
  menuItems: Required<MenuProps>['items'];

  /**
   * 菜單折疊狀態
   */
  collapsed: boolean;

  /**
   * 展開的子菜單 key 列表
   */
  openKeys: string[];

  /**
   * 當前選中的菜單 key
   */
  selectedKeys: string[];

  /**
   * 切換菜單折疊狀態
   */
  handleToggleCollapsed: () => void;

  /**
   * 處理子菜單展開/關閉
   */
  handleOpenChange: (keys: string[]) => void;

  /**
   * 處理菜單項點擊
   */
  handleMenuClick: (menuId: string) => void;
}

/**
 * 將 MenuItem[] 轉換為 Ant Design Menu 的數據格式
 */
function convertToMenuItems(menuList: MenuItem[]): Required<MenuProps>['items'] {
  return menuList.map((menu) => ({
    key: menu.menuId,
    label: menu.menuName,
    icon: menu.icon ? undefined : undefined, // TODO: 實現圖標映射
    children: menu.children && menu.children.length > 0 ? convertToMenuItems(menu.children) : undefined,
  }));
}

/**
 * 根據路徑找到對應的菜單項 ID
 *
 * 優先匹配更具體的路徑（先遞迴查找子菜單，再匹配當前菜單）
 */
function findMenuIdByPath(menuList: MenuItem[], pathname: string): string {
  for (const menu of menuList) {
    // 先遞迴查找子菜單（優先匹配更具體的路徑）
    if (menu.children && menu.children.length > 0) {
      const foundId = findMenuIdByPath(menu.children, pathname);
      if (foundId) {
        return foundId;
      }
    }

    // 再匹配當前路徑
    if (menu.path && pathname.startsWith(menu.path)) {
      return menu.menuId;
    }
  }
  return '';
}

/**
 * 獲取父菜單 ID 列表（用於展開父級）
 */
function getParentKeys(menuList: MenuItem[], targetId: string): string[] {
  const parentKeys: string[] = [];

  function findParents(list: MenuItem[], targetId: string): boolean {
    for (const menu of list) {
      if (menu.menuId === targetId) {
        return true;
      }

      if (menu.children && menu.children.length > 0) {
        if (findParents(menu.children, targetId)) {
          parentKeys.unshift(menu.menuId);
          return true;
        }
      }
    }
    return false;
  }

  findParents(menuList, targetId);
  return parentKeys;
}

/**
 * useMenu Hook
 */
export function useMenu(): UseMenuReturn {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();

  // 從 Redux Store 獲取菜單樹
  const menuTree = useSelector(selectMenuTree);
  const collapsed = useSelector(selectCollapsed);
  const openKeys = useSelector(selectOpenKeys);
  const selectedMenuId = useSelector(selectSelectedMenuId);

  /**
   * 轉換菜單數據格式（Ant Design Menu 格式）
   */
  const menuItems = useMemo(() => {
    return convertToMenuItems(menuTree);
  }, [menuTree]);

  /**
   * 根據當前路由自動選中菜單項
   */
  useMemo(() => {
    const currentMenuId = findMenuIdByPath(menuTree, location.pathname);
    if (currentMenuId && currentMenuId !== selectedMenuId) {
      dispatch(setSelectedMenuId(currentMenuId));

      // 自動展開父級菜單
      if (!collapsed) {
        const parentKeys = getParentKeys(menuTree, currentMenuId);
        if (parentKeys.length > 0) {
          dispatch(setOpenKeys(parentKeys));
        }
      }
    }
  }, [location.pathname, menuTree, selectedMenuId, collapsed, dispatch]);

  /**
   * 切換菜單折疊狀態
   */
  const handleToggleCollapsed = useCallback(() => {
    dispatch(toggleCollapsed());
  }, [dispatch]);

  /**
   * 處理子菜單展開/關閉
   */
  const handleOpenChange = useCallback(
    (keys: string[]) => {
      dispatch(setOpenKeys(keys));
    },
    [dispatch]
  );

  /**
   * 處理菜單項點擊
   */
  const handleMenuClick = useCallback(
    (menuId: string) => {
      // 找到對應的菜單項
      function findMenuItem(list: MenuItem[], id: string): MenuItem | null {
        for (const menu of list) {
          if (menu.menuId === id) {
            return menu;
          }
          if (menu.children) {
            const found = findMenuItem(menu.children, id);
            if (found) return found;
          }
        }
        return null;
      }

      const menuItem = findMenuItem(menuTree, menuId);
      if (menuItem && menuItem.path) {
        // 更新選中狀態
        dispatch(setSelectedMenuId(menuId));
        // 導航到對應路徑
        navigate(menuItem.path);
      }
    },
    [menuTree, navigate, dispatch]
  );

  return {
    menuItems,
    collapsed,
    openKeys,
    selectedKeys: selectedMenuId ? [selectedMenuId] : [],
    handleToggleCollapsed,
    handleOpenChange,
    handleMenuClick,
  };
}

/**
 * 導出工具函數供其他 Hook 使用（如 useBreadcrumb）
 */
export { findMenuIdByPath, getParentKeys };
