/**
 * Breadcrumb 單元測試
 *
 * 測試覆蓋：
 * 1. Breadcrumb 正確渲染
 * 2. 根據路由自動生成麵包屑
 * 3. 點擊麵包屑項跳轉路由
 * 4. 邊界情況處理
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { describe, test, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/test/utils/test-utils';
import type { MenuItem } from '@/types/user.types';
import Breadcrumb from '../Breadcrumb';

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

describe('Breadcrumb', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockLocation = { pathname: '/home' };
  });

  /**
   * 測試 1：應該正確渲染 Breadcrumb 組件
   */
  test('應該正確渲染 Breadcrumb 組件', () => {
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    // 驗證：麵包屑應該被渲染
    expect(screen.getByText('首頁')).toBeInTheDocument();
  });

  /**
   * 測試 2：應該始終顯示「首頁」作為第一項
   */
  test('應該始終顯示「首頁」作為第一項', () => {
    mockLocation = { pathname: '/player/list' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    // 驗證：「首頁」應該始終存在
    expect(screen.getByText('首頁')).toBeInTheDocument();
  });

  /**
   * 測試 3：應該為首頁顯示 HomeOutlined 圖標
   */
  test('應該為首頁顯示 HomeOutlined 圖標', () => {
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    // 驗證：首頁應該包含 icon class（HomeOutlined）
    const homeIcon = document.querySelector('.anticon-home');
    expect(homeIcon).toBeInTheDocument();
  });

  /**
   * 測試 4：根路由（/home）應該僅顯示「首頁」
   */
  test('根路由（/home）應該僅顯示「首頁」', () => {
    mockLocation = { pathname: '/home' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    // 驗證：僅顯示「首頁」
    expect(screen.getByText('首頁')).toBeInTheDocument();
    expect(screen.queryByText('玩家管理')).not.toBeInTheDocument();
  });

  /**
   * 測試 5：一級菜單路由應該顯示「首頁 > 菜單名稱」
   */
  test('一級菜單路由應該顯示「首頁 > 菜單名稱」', () => {
    mockLocation = { pathname: '/player' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    expect(screen.getByText('首頁')).toBeInTheDocument();
    expect(screen.getByText('玩家管理')).toBeInTheDocument();
  });

  /**
   * 測試 6：二級菜單路由應該顯示「首頁 > 父菜單 > 當前菜單」
   */
  test('二級菜單路由應該顯示「首頁 > 父菜單 > 當前菜單」', () => {
    mockLocation = { pathname: '/player/list' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    expect(screen.getByText('首頁')).toBeInTheDocument();
    expect(screen.getByText('玩家管理')).toBeInTheDocument();
    expect(screen.getByText('玩家列表')).toBeInTheDocument();
  });

  /**
   * 測試 7：最後一項麵包屑應該使用當前頁樣式（不可點擊）
   */
  test('最後一項麵包屑應該使用當前頁樣式（不可點擊）', () => {
    mockLocation = { pathname: '/player/list' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    const lastItem = screen.getByText('玩家列表');

    // 驗證：最後一項應該有 breadcrumb-item-current class
    expect(lastItem).toHaveClass('breadcrumb-item-current');
  });

  /**
   * 測試 8：點擊「首頁」應該跳轉到 /home
   */
  test('點擊「首頁」應該跳轉到 /home', () => {
    mockLocation = { pathname: '/player/list' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    const homeLink = screen.getByText('首頁');
    fireEvent.click(homeLink);

    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });

  /**
   * 測試 9：點擊父菜單項應該跳轉到對應路徑
   */
  test('點擊父菜單項應該跳轉到對應路徑', () => {
    mockLocation = { pathname: '/player/list' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    const parentLink = screen.getByText('玩家管理');
    fireEvent.click(parentLink);

    expect(mockNavigate).toHaveBeenCalledWith('/player');
  });

  /**
   * 測試 10：點擊最後一項（當前頁）不應該觸發導航
   */
  test('點擊最後一項（當前頁）不應該觸發導航', () => {
    mockLocation = { pathname: '/player/list' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    const currentPageItem = screen.getByText('玩家列表');
    fireEvent.click(currentPageItem);

    // 驗證：不應該調用 navigate
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  /**
   * 測試 11：無匹配菜單時應該僅顯示「首頁」（優雅降級）
   */
  test('無匹配菜單時應該僅顯示「首頁」', () => {
    mockLocation = { pathname: '/unknown/path' };
    const menuTree = createMockMenuTree();
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree },
      },
    });

    // 驗證：僅顯示「首頁」
    expect(screen.getByText('首頁')).toBeInTheDocument();
    expect(screen.queryByText('玩家管理')).not.toBeInTheDocument();
  });

  /**
   * 測試 12：空菜單樹時應該僅顯示「首頁」
   */
  test('空菜單樹時應該僅顯示「首頁」', () => {
    mockLocation = { pathname: '/player/list' };
    renderWithProviders(<Breadcrumb />, {
      preloadedState: {
        user: { menuTree: [] }, // 空菜單樹
      },
    });

    // 驗證：僅顯示「首頁」
    expect(screen.getByText('首頁')).toBeInTheDocument();
    expect(screen.queryByText('玩家管理')).not.toBeInTheDocument();
  });
});
