/**
 * usePrivilege Hook 單元測試
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { renderHook } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { usePrivilege, usePrivileges } from '../usePrivilege';
import userReducer from '@/store/slices/userSlice';
import type { UserState } from '@/store/slices/userSlice';
import type { PermissionPoint } from '@/types/menu';

// ================================ Mock Store Factory ================================

/**
 * 創建測試用 Redux Store
 */
function createTestStore(overrides?: {
  administratorFlag?: boolean;
  pointsList?: PermissionPoint[];
}) {
  const preloadedUserState: UserState = {
    token: 'test-token',
    employeeId: 'test-employee-id',
    employeeName: 'Test User',
    loginName: 'testuser',
    administratorFlag: overrides?.administratorFlag ?? false,
    pointsList: overrides?.pointsList ?? [],
    menuTree: [],
    displayMenuTree: [],
    menuRouterList: [],
    menuParentIdListMap: {},
    unreadMessageCount: 0,
    loading: false,
    error: null,
  };

  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: preloadedUserState,
    },
  });
}

/**
 * 測試用權限點數據（matches PermissionPoint interface from @/types/menu）
 */
const mockPermissions: PermissionPoint[] = [
  { menuId: '1', webPerms: 'system:user:add', menuName: '用戶新增' },
  { menuId: '2', webPerms: 'system:user:edit', menuName: '用戶編輯' },
  { menuId: '3', webPerms: 'business:goods:query', menuName: '商品查看' },
];

// ================================ usePrivilege Tests ================================

describe('usePrivilege', () => {
  test('超級管理員應該擁有所有權限', () => {
    const store = createTestStore({ administratorFlag: true, pointsList: [] });

    const { result } = renderHook(() => usePrivilege('system:user:delete'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(true);
  });

  test('普通用戶有權限時應該返回 true', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivilege('system:user:add'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(true);
  });

  test('普通用戶無權限時應該返回 false', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivilege('system:user:delete'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(false);
  });

  test('空權限列表應該返回 false', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: [] });

    const { result } = renderHook(() => usePrivilege('system:user:add'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(false);
  });

  test('權限編碼完全匹配時應該返回 true', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivilege('business:goods:query'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(true);
  });

  test('權限編碼部分匹配時應該返回 false（精確匹配）', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivilege('system:user'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(false);
  });
});

// ================================ usePrivileges Tests ================================

describe('usePrivileges', () => {
  test('超級管理員應該對所有權限返回 true', () => {
    const store = createTestStore({ administratorFlag: true, pointsList: [] });

    const { result } = renderHook(
      () =>
        usePrivileges([
          'system:user:add',
          'system:user:edit',
          'system:user:delete',
          'business:goods:query',
        ]),
      {
        wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
      }
    );

    // usePrivileges returns boolean (all permissions must be granted)
    expect(result.current).toBe(true);
  });

  test('普通用戶應該根據權限列表返回正確結果', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    // User has: system:user:add, system:user:edit, business:goods:query
    // User does NOT have: system:user:delete
    const { result } = renderHook(
      () =>
        usePrivileges([
          'system:user:add',
          'system:user:edit',
          'system:user:delete', // missing
          'business:goods:query',
        ]),
      {
        wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
      }
    );

    // Not all permissions granted, so returns false
    expect(result.current).toBe(false);
  });

  test('空權限編碼數組應該返回 true (vacuously true)', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivileges([]), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    // Array.every on empty array returns true
    expect(result.current).toBe(true);
  });

  test('空權限列表應該對所有權限返回 false', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: [] });

    const { result } = renderHook(
      () => usePrivileges(['system:user:add', 'system:user:edit', 'system:user:delete']),
      {
        wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
      }
    );

    expect(result.current).toBe(false);
  });

  test('單個權限編碼應該正確檢查', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivileges(['system:user:add']), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(true);
  });
});
