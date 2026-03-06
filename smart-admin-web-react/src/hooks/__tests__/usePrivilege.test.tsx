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
import type { MenuPoint } from '@/types/user.types';

// ================================ Mock Store Factory ================================

/**
 * 創建測試用 Redux Store
 */
function createTestStore(overrides?: {
  administratorFlag?: boolean;
  pointsList?: MenuPoint[];
}) {
  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: {
        token: 'test-token',
        employeeId: 'test-employee-id',
        employeeName: 'Test User',
        administratorFlag: overrides?.administratorFlag ?? false,
        pointsList: overrides?.pointsList ?? [],
        menuTree: [],
      },
    },
  });
}

/**
 * 測試用權限點數據
 */
const mockPermissions: MenuPoint[] = [
  {
    menuId: '1',
    menuName: '用戶新增',
    webPerms: 'system:user:add',
    visibleFlag: true,
    disabledFlag: false,
  },
  {
    menuId: '2',
    menuName: '用戶編輯',
    webPerms: 'system:user:edit',
    visibleFlag: true,
    disabledFlag: false,
  },
  {
    menuId: '3',
    menuName: '商品查看',
    webPerms: 'business:goods:query',
    visibleFlag: true,
    disabledFlag: false,
  },
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

  test('權限列表為 null 時應該返回 false', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: undefined as any });

    const { result } = renderHook(() => usePrivilege('system:user:add'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(false);
  });

  test('多個權限點包含相同 webPerms 時應該正確檢查', () => {
    const duplicatePermissions: MenuPoint[] = [
      {
        menuId: '1',
        menuName: '用戶新增',
        webPerms: 'system:user:add',
        visibleFlag: true,
        disabledFlag: false,
      },
      {
        menuId: '2',
        menuName: '用戶新增（副本）',
        webPerms: 'system:user:add',
        visibleFlag: true,
        disabledFlag: false,
      },
    ];

    const store = createTestStore({ administratorFlag: false, pointsList: duplicatePermissions });

    const { result } = renderHook(() => usePrivilege('system:user:add'), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toBe(true);
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

    expect(result.current).toEqual({
      'system:user:add': true,
      'system:user:edit': true,
      'system:user:delete': true,
      'business:goods:query': true,
    });
  });

  test('普通用戶應該根據權限列表返回正確結果', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(
      () =>
        usePrivileges([
          'system:user:add', // 有權限
          'system:user:edit', // 有權限
          'system:user:delete', // 無權限
          'business:goods:query', // 有權限
        ]),
      {
        wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
      }
    );

    expect(result.current).toEqual({
      'system:user:add': true,
      'system:user:edit': true,
      'system:user:delete': false,
      'business:goods:query': true,
    });
  });

  test('空權限編碼數組應該返回空對象', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivileges([]), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toEqual({});
  });

  test('空權限列表應該對所有權限返回 false', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: [] });

    const { result } = renderHook(
      () => usePrivileges(['system:user:add', 'system:user:edit', 'system:user:delete']),
      {
        wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
      }
    );

    expect(result.current).toEqual({
      'system:user:add': false,
      'system:user:edit': false,
      'system:user:delete': false,
    });
  });

  test('單個權限編碼應該正確檢查', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { result } = renderHook(() => usePrivileges(['system:user:add']), {
      wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
    });

    expect(result.current).toEqual({
      'system:user:add': true,
    });
  });
});
