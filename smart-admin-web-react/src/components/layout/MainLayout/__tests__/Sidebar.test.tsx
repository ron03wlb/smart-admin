/**
 * Sidebar 單元測試
 *
 * 測試覆蓋：
 * 1. Sidebar 正確渲染
 * 2. 菜單樹正確渲染（基於 state.user.menuTree）
 * 3. 點擊折疊按鈕切換 collapsed 狀態
 * 4. 折疊狀態持久化（Redux Store）
 * 5. 當前路由對應的菜單項高亮
 * 6. 點擊菜單項跳轉路由
 * 7. 折疊狀態下不顯示子菜單
 * 8. 展開狀態下顯示子菜單
 * 9. 空菜單樹時不渲染菜單項
 * 10. 自動展開父級菜單（根據當前路由）
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { describe, test, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils/test-utils';
import type { MenuItem } from '@/types/user.types';
import Sidebar from '../Sidebar';

// Mock react-router-dom
const mockNavigate = vi.fn();
let mockLocation = { pathname: '/home' };

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => mockLocation,
  };
});

// Mock 菜單樹數據
const createMockMenuTree = (): MenuItem[] => [
  {
    menuId: '1',
    menuName: '玩家管理',
    menuType: 'CATALOG',
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
    children: [
      {
        menuId: '11',
        menuName: '玩家列表',
        menuType: 'MENU',
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
        menuId: '12',
        menuName: '玩家詳情',
        menuType: 'MENU',
        webPerms: 'player:detail',
        visibleFlag: true,
        disabledFlag: false,
        parentId: '1',
        path: '/player/detail',
        component: 'PlayerDetail',
        icon: '',
        sort: 2,
        permsType: 1,
        apiPerms: '',
        frameSrc: '',
        frameFlag: false,
        cacheFlag: false,
        contextMenuId: '',
      },
    ],
  },
  {
    menuId: '2',
    menuName: '遊戲管理',
    menuType: 'CATALOG',
    webPerms: '',
    visibleFlag: true,
    disabledFlag: false,
    parentId: '0',
    path: '/game',
    component: '',
    icon: 'GamepadOutlined',
    sort: 2,
    permsType: 1,
    apiPerms: '',
    frameSrc: '',
    frameFlag: false,
    cacheFlag: false,
    contextMenuId: '',
    children: [
      {
        menuId: '21',
        menuName: '遊戲列表',
        menuType: 'MENU',
        webPerms: 'game:list',
        visibleFlag: true,
        disabledFlag: false,
        parentId: '2',
        path: '/game/list',
        component: 'GameList',
        icon: '',
        sort: 1,
        permsType: 1,
        apiPerms: '',
        frameSrc: '',
        frameFlag: false,
        cacheFlag: false,
        contextMenuId: '',
      },
    ],
  },
  {
    menuId: '3',
    menuName: '系統設置',
    menuType: 'MENU',
    webPerms: 'system:settings',
    visibleFlag: true,
    disabledFlag: false,
    parentId: '0',
    path: '/system/settings',
    component: 'SystemSettings',
    icon: 'SettingOutlined',
    sort: 3,
    permsType: 1,
    apiPerms: '',
    frameSrc: '',
    frameFlag: false,
    cacheFlag: false,
    contextMenuId: '',
  },
];

describe('Sidebar', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockLocation = { pathname: '/home' };
  });

  /**
   * 測試 1：應該正確渲染 Sidebar 組件
   */
  test('應該正確渲染 Sidebar 組件', () => {
    const menuTree = createMockMenuTree();
    renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 驗證：Sidebar 組件應該被渲染
    expect(screen.getByText('玩家管理')).toBeInTheDocument();
    expect(screen.getByText('遊戲管理')).toBeInTheDocument();
    expect(screen.getByText('系統設置')).toBeInTheDocument();
  });

  /**
   * 測試 2：應該正確渲染菜單樹（包含子菜單）
   */
  test('應該正確渲染菜單樹（包含子菜單）', () => {
    const menuTree = createMockMenuTree();
    renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: ['1'], // 展開「玩家管理」
        },
      },
    });

    // 驗證：父菜單應該被渲染
    expect(screen.getByText('玩家管理')).toBeInTheDocument();

    // 驗證：子菜單應該被渲染（當 openKeys 包含父菜單 ID 時）
    expect(screen.getByText('玩家列表')).toBeInTheDocument();
    expect(screen.getByText('玩家詳情')).toBeInTheDocument();
  });

  /**
   * 測試 3：點擊折疊按鈕應該切換 collapsed 狀態
   */
  test('點擊折疊按鈕應該切換 collapsed 狀態', () => {
    const menuTree = createMockMenuTree();
    const { store } = renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 1. 初始狀態：未折疊
    expect(store.getState().menu.collapsed).toBe(false);

    // 2. 找到折疊按鈕
    const triggerButton = screen.getByTestId('sidebar-trigger');
    expect(triggerButton).toBeInTheDocument();

    // 3. 點擊折疊按鈕
    fireEvent.click(triggerButton);

    // 4. 驗證：collapsed 狀態應該變為 true
    expect(store.getState().menu.collapsed).toBe(true);

    // 5. 再次點擊折疊按鈕
    fireEvent.click(triggerButton);

    // 6. 驗證：collapsed 狀態應該變為 false
    expect(store.getState().menu.collapsed).toBe(false);
  });

  /**
   * 測試 4：折疊狀態下應該清空 openKeys（自動關閉子菜單）
   */
  test('折疊狀態下應該清空 openKeys（自動關閉子菜單）', () => {
    const menuTree = createMockMenuTree();
    const { store } = renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: ['1'], // 初始展開「玩家管理」
        },
      },
    });

    // 1. 初始狀態：openKeys 包含 '1'
    expect(store.getState().menu.openKeys).toEqual(['1']);

    // 2. 點擊折疊按鈕
    const triggerButton = screen.getByTestId('sidebar-trigger');
    fireEvent.click(triggerButton);

    // 3. 驗證：折疊後 openKeys 應該被清空
    expect(store.getState().menu.collapsed).toBe(true);
    expect(store.getState().menu.openKeys).toEqual([]);
  });

  /**
   * 測試 5：當前路由對應的菜單項應該高亮
   */
  test('當前路由對應的菜單項應該高亮', async () => {
    const menuTree = createMockMenuTree();
    mockLocation = { pathname: '/player/list' };

    const { store } = renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 等待 useMenu Hook 自動設置 selectedMenuId
    await waitFor(() => {
      expect(store.getState().menu.selectedMenuId).toBe('11');
    });

    // 驗證：openKeys 應該自動包含父菜單 ID
    await waitFor(() => {
      expect(store.getState().menu.openKeys).toEqual(['1']);
    });
  });

  /**
   * 測試 6：點擊菜單項應該跳轉路由
   */
  test('點擊菜單項應該跳轉路由', async () => {
    const menuTree = createMockMenuTree();
    const { store } = renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: ['1'], // 展開「玩家管理」
        },
      },
    });

    // 1. 找到「玩家列表」菜單項
    const playerListMenuItem = screen.getByText('玩家列表');
    expect(playerListMenuItem).toBeInTheDocument();

    // 2. 點擊「玩家列表」菜單項
    fireEvent.click(playerListMenuItem);

    // 3. 驗證：應該調用 navigate('/player/list')
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/player/list');
    });

    // 4. 驗證：selectedMenuId 應該更新為 '11'
    await waitFor(() => {
      expect(store.getState().menu.selectedMenuId).toBe('11');
    });
  });

  /**
   * 測試 7：點擊沒有 path 的菜單項不應該跳轉
   */
  test('點擊沒有 path 的菜單項不應該跳轉', () => {
    const menuTree: MenuItem[] = [
      {
        menuId: '1',
        menuName: '測試菜單（無路徑）',
        menuType: 'MENU',
        webPerms: '',
        visibleFlag: true,
        disabledFlag: false,
        parentId: '0',
        path: '', // 無路徑
        component: '',
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

    renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 1. 找到「測試菜單（無路徑）」菜單項
    const menuItem = screen.getByText('測試菜單（無路徑）');

    // 2. 點擊菜單項
    fireEvent.click(menuItem);

    // 3. 驗證：不應該調用 navigate
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  /**
   * 測試 8：空菜單樹時不應該渲染菜單項
   */
  test('空菜單樹時不應該渲染菜單項', () => {
    renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree: [], // 空菜單樹
        },
        menu: {
          collapsed: false,
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 驗證：不應該有菜單項被渲染
    expect(screen.queryByText('玩家管理')).not.toBeInTheDocument();
    expect(screen.queryByText('遊戲管理')).not.toBeInTheDocument();
  });

  /**
   * 測試 9：展開狀態下應該顯示折疊圖標（MenuFoldOutlined）
   */
  test('展開狀態下應該顯示折疊圖標（MenuFoldOutlined）', () => {
    const menuTree = createMockMenuTree();
    renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: false, // 展開狀態
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 驗證：應該顯示 MenuFoldOutlined 圖標
    const foldIcon = document.querySelector('.anticon-menu-fold');
    expect(foldIcon).toBeInTheDocument();
  });

  /**
   * 測試 10：折疊狀態下應該顯示展開圖標（MenuUnfoldOutlined）
   */
  test('折疊狀態下應該顯示展開圖標（MenuUnfoldOutlined）', () => {
    const menuTree = createMockMenuTree();
    renderWithProviders(<Sidebar />, {
      preloadedState: {
        user: {
          menuTree,
        },
        menu: {
          collapsed: true, // 折疊狀態
          selectedMenuId: '',
          openKeys: [],
        },
      },
    });

    // 驗證：應該顯示 MenuUnfoldOutlined 圖標
    const unfoldIcon = document.querySelector('.anticon-menu-unfold');
    expect(unfoldIcon).toBeInTheDocument();
  });
});
