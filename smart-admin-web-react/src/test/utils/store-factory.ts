/**
 * Redux Store 工廠函數（測試專用）
 *
 * 功能：
 * 1. createTestStore() - 創建隔離的測試 Store（無 Redux Persist）
 * 2. createMockUserState() - 創建測試用 UserState
 *
 * 使用場景：
 * - 單元測試：使用 createTestStore()（快速、隔離）
 * - 集成測試：使用真實 Store（src/store/index.ts）
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/slices/userSlice';
import menuReducer from '@/store/slices/menuSlice';
import type { UserState } from '@/types/user.types';
import type { MenuState } from '@/store/slices/menuSlice';

/**
 * 創建測試用 UserState
 *
 * @param overrides - 可選覆寫字段
 * @returns 完整的 UserState 對象
 *
 * @example
 * ```typescript
 * const userState = createMockUserState({
 *   administratorFlag: true,
 *   employeeName: 'Admin User',
 * });
 * ```
 */
export function createMockUserState(overrides?: Partial<UserState>): UserState {
  return {
    token: 'test-token',
    employeeId: 'test-employee-id',
    employeeName: 'Test User',
    administratorFlag: false,
    pointsList: [],
    menuTree: [],
    departmentId: undefined,
    departmentName: undefined,
    ...overrides,
  };
}

/**
 * 創建測試用 MenuState
 *
 * @param overrides - 可選覆寫字段
 * @returns 完整的 MenuState 對象
 *
 * @example
 * ```typescript
 * const menuState = createMockMenuState({
 *   collapsed: true,
 *   selectedMenuId: '11',
 * });
 * ```
 */
export function createMockMenuState(overrides?: Partial<MenuState>): MenuState {
  return {
    collapsed: false,
    selectedMenuId: '',
    openKeys: [],
    ...overrides,
  };
}

/**
 * 測試 Store 的 PreloadedState 類型
 */
export interface TestStorePreloadedState {
  user?: Partial<UserState>;
  menu?: Partial<MenuState>;
}

/**
 * 創建隔離的測試 Store（無 Redux Persist）
 *
 * @param overrides - 可選覆寫 UserState 和 MenuState 字段
 * @returns 測試用 Redux Store
 *
 * @example
 * ```typescript
 * // 創建超級管理員 Store
 * const adminStore = createTestStore({
 *   user: { administratorFlag: true },
 * });
 *
 * // 創建有權限的 Store
 * const storeWithPermissions = createTestStore({
 *   user: {
 *     pointsList: [
 *       { webPerms: 'system:user:add', ... },
 *       { webPerms: 'system:user:edit', ... },
 *     ],
 *   },
 * });
 *
 * // 創建折疊菜單的 Store
 * const storeWithCollapsedMenu = createTestStore({
 *   menu: { collapsed: true, selectedMenuId: '11' },
 * });
 * ```
 */
export function createTestStore(overrides?: TestStorePreloadedState | Partial<UserState>) {
  // 兼容舊的 API（直接傳入 Partial<UserState>）
  let userOverrides: Partial<UserState> = {};
  let menuOverrides: Partial<MenuState> = {};

  if (overrides) {
    // 檢查是否為新的 TestStorePreloadedState 格式
    if ('user' in overrides || 'menu' in overrides) {
      const typedOverrides = overrides as TestStorePreloadedState;
      userOverrides = typedOverrides.user || {};
      menuOverrides = typedOverrides.menu || {};
    } else {
      // 舊的 API（直接傳入 Partial<UserState>）
      userOverrides = overrides as Partial<UserState>;
    }
  }

  return configureStore({
    reducer: {
      user: userReducer,
      menu: menuReducer,
    },
    preloadedState: {
      user: createMockUserState(userOverrides),
      menu: createMockMenuState(menuOverrides),
    },
  });
}
