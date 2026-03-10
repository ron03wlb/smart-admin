import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { usePrivilege, usePrivileges, useAnyPrivilege } from './usePrivilege';
import userReducer from '../store/slices/userSlice';

interface TestUserState {
  administratorFlag: boolean;
  pointsList: Array<{ menuId: number; webPerms: string }>;
}

// 創建測試用的 Redux store
const createTestStore = (userState: TestUserState) => {
  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: {
        token: '',
        employeeId: '',
        employeeName: '',
        loginName: '',
        administratorFlag: userState.administratorFlag,
        menuTree: [],
        displayMenuTree: [],
        pointsList: userState.pointsList,
        menuRouterList: [],
        menuParentIdListMap: {},
        loading: false,
        error: null,
      },
    },
  });
};

// 測試用的 wrapper
const createWrapper = (store: any) => {
  return ({ children }: { children: React.ReactNode }) => (
    <Provider store={store}>{children}</Provider>
  );
};

describe('usePrivilege', () => {
  describe('管理員用戶', () => {
    it('應該返回 true（管理員擁有所有權限）', () => {
      const store = createTestStore({
        administratorFlag: true,
        pointsList: [],
      });

      const { result } = renderHook(() => usePrivilege('goods:add'), {
        wrapper: createWrapper(store),
      });

      expect(result.current).toBe(true);
    });
  });

  describe('普通用戶', () => {
    it('應該返回 true（用戶擁有該權限）', () => {
      const store = createTestStore({
        administratorFlag: false,
        pointsList: [
          { menuId: 1, webPerms: 'goods:add' },
          { menuId: 2, webPerms: 'goods:edit' },
        ],
      });

      const { result } = renderHook(() => usePrivilege('goods:add'), {
        wrapper: createWrapper(store),
      });

      expect(result.current).toBe(true);
    });

    it('應該返回 false（用戶沒有該權限）', () => {
      const store = createTestStore({
        administratorFlag: false,
        pointsList: [{ menuId: 1, webPerms: 'goods:add' }],
      });

      const { result } = renderHook(() => usePrivilege('goods:delete'), {
        wrapper: createWrapper(store),
      });

      expect(result.current).toBe(false);
    });

    it('應該返回 false（用戶沒有任何權限）', () => {
      const store = createTestStore({
        administratorFlag: false,
        pointsList: [],
      });

      const { result } = renderHook(() => usePrivilege('goods:add'), {
        wrapper: createWrapper(store),
      });

      expect(result.current).toBe(false);
    });
  });
});

describe('usePrivileges', () => {
  it('應該返回 true（管理員擁有所有權限）', () => {
    const store = createTestStore({
      administratorFlag: true,
      pointsList: [],
    });

    const { result } = renderHook(() => usePrivileges(['goods:add', 'goods:edit']), {
      wrapper: createWrapper(store),
    });

    expect(result.current).toBe(true);
  });

  it('應該返回 true（用戶擁有所有權限）', () => {
    const store = createTestStore({
      administratorFlag: false,
      pointsList: [
        { menuId: 1, webPerms: 'goods:add' },
        { menuId: 2, webPerms: 'goods:edit' },
        { menuId: 3, webPerms: 'goods:delete' },
      ],
    });

    const { result } = renderHook(() => usePrivileges(['goods:add', 'goods:edit']), {
      wrapper: createWrapper(store),
    });

    expect(result.current).toBe(true);
  });

  it('應該返回 false（用戶缺少部分權限）', () => {
    const store = createTestStore({
      administratorFlag: false,
      pointsList: [{ menuId: 1, webPerms: 'goods:add' }],
    });

    const { result } = renderHook(() => usePrivileges(['goods:add', 'goods:edit']), {
      wrapper: createWrapper(store),
    });

    expect(result.current).toBe(false);
  });
});

describe('useAnyPrivilege', () => {
  it('應該返回 true（管理員擁有所有權限）', () => {
    const store = createTestStore({
      administratorFlag: true,
      pointsList: [],
    });

    const { result } = renderHook(() => useAnyPrivilege(['goods:add', 'goods:edit']), {
      wrapper: createWrapper(store),
    });

    expect(result.current).toBe(true);
  });

  it('應該返回 true（用戶擁有其中一個權限）', () => {
    const store = createTestStore({
      administratorFlag: false,
      pointsList: [{ menuId: 1, webPerms: 'goods:add' }],
    });

    const { result } = renderHook(() => useAnyPrivilege(['goods:add', 'goods:edit']), {
      wrapper: createWrapper(store),
    });

    expect(result.current).toBe(true);
  });

  it('應該返回 false（用戶沒有任何一個權限）', () => {
    const store = createTestStore({
      administratorFlag: false,
      pointsList: [{ menuId: 4, webPerms: 'goods:query' }],
    });

    const { result } = renderHook(() => useAnyPrivilege(['goods:add', 'goods:edit']), {
      wrapper: createWrapper(store),
    });

    expect(result.current).toBe(false);
  });
});
