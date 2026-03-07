/**
 * Redux Store 工廠函數（測試專用）
 *
 * 功能：
 * 1. createTestStore() - 創建隔離的測試 Store（無 Redux Persist）
 * 2. createMock*State() - 創建各 slice 的測試狀態
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
import tagNavReducer from '@/store/slices/tagNavSlice';
import appConfigReducer, { APP_CONFIG_DEFAULTS } from '@/store/slices/appConfigSlice';
import dictReducer from '@/store/slices/dictSlice';
import spinReducer from '@/store/slices/spinSlice';
import tenantReducer from '@/store/slices/tenantSlice';
import type { UserState } from '@/types/user.types';
import type { MenuState } from '@/store/slices/menuSlice';
import type { TagNavState } from '@/store/slices/tagNavSlice';
import type { AppConfigState } from '@/types/app-config.types';
import type { DictState } from '@/store/slices/dictSlice';
import type { SpinState } from '@/store/slices/spinSlice';
import type { TenantState } from '@/store/slices/tenantSlice';

export function createMockUserState(overrides?: Partial<UserState>): UserState {
  return {
    token: 'test-token',
    employeeId: 'test-employee-id',
    employeeName: 'Test User',
    administratorFlag: false,
    pointsList: [],
    menuTree: [],
    menuRouterList: [],
    departmentId: undefined,
    departmentName: undefined,
    ...overrides,
  };
}

export function createMockMenuState(overrides?: Partial<MenuState>): MenuState {
  return {
    collapsed: false,
    selectedMenuId: '',
    openKeys: [],
    ...overrides,
  };
}

export function createMockTagNavState(overrides?: Partial<TagNavState>): TagNavState {
  return {
    tagList: [],
    activeKey: '',
    ...overrides,
  };
}

export function createMockAppConfigState(overrides?: Partial<AppConfigState>): AppConfigState {
  return {
    ...APP_CONFIG_DEFAULTS,
    ...overrides,
  };
}

export function createMockDictState(overrides?: Partial<DictState>): DictState {
  return {
    dictList: [],
    dictMap: {},
    ...overrides,
  };
}

export function createMockSpinState(overrides?: Partial<SpinState>): SpinState {
  return {
    loading: false,
    ...overrides,
  };
}

export function createMockTenantState(overrides?: Partial<TenantState>): TenantState {
  return {
    tenantId: '',
    tenantName: '',
    timezone: 'UTC',
    ...overrides,
  };
}

/**
 * 測試 Store 的 PreloadedState 類型
 */
export interface TestStorePreloadedState {
  user?: Partial<UserState>;
  menu?: Partial<MenuState>;
  tagNav?: Partial<TagNavState>;
  appConfig?: Partial<AppConfigState>;
  dict?: Partial<DictState>;
  spin?: Partial<SpinState>;
  tenant?: Partial<TenantState>;
}

/**
 * 創建隔離的測試 Store（無 Redux Persist）
 */
export function createTestStore(overrides?: TestStorePreloadedState | Partial<UserState>) {
  // 兼容舊的 API（直接傳入 Partial<UserState>）
  let userOverrides: Partial<UserState> = {};
  let menuOverrides: Partial<MenuState> = {};
  let tagNavOverrides: Partial<TagNavState> = {};
  let appConfigOverrides: Partial<AppConfigState> = {};
  let dictOverrides: Partial<DictState> = {};
  let spinOverrides: Partial<SpinState> = {};
  let tenantOverrides: Partial<TenantState> = {};

  if (overrides) {
    const sliceKeys = ['user', 'menu', 'tagNav', 'appConfig', 'dict', 'spin', 'tenant'];
    if (sliceKeys.some((key) => key in overrides)) {
      const typedOverrides = overrides as TestStorePreloadedState;
      userOverrides = typedOverrides.user || {};
      menuOverrides = typedOverrides.menu || {};
      tagNavOverrides = typedOverrides.tagNav || {};
      appConfigOverrides = typedOverrides.appConfig || {};
      dictOverrides = typedOverrides.dict || {};
      spinOverrides = typedOverrides.spin || {};
      tenantOverrides = typedOverrides.tenant || {};
    } else {
      userOverrides = overrides as Partial<UserState>;
    }
  }

  return configureStore({
    reducer: {
      user: userReducer,
      menu: menuReducer,
      tagNav: tagNavReducer,
      appConfig: appConfigReducer,
      dict: dictReducer,
      spin: spinReducer,
      tenant: tenantReducer,
    },
    preloadedState: {
      user: createMockUserState(userOverrides),
      menu: createMockMenuState(menuOverrides),
      tagNav: createMockTagNavState(tagNavOverrides),
      appConfig: createMockAppConfigState(appConfigOverrides),
      dict: createMockDictState(dictOverrides),
      spin: createMockSpinState(spinOverrides),
      tenant: createMockTenantState(tenantOverrides),
    },
  });
}
