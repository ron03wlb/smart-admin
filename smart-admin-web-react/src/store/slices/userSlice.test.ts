/**
 * userSlice Tests
 * 用戶狀態管理測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import userReducer, {
  login,
  getLoginInfo,
  logout,
  setToken,
  setUserInfo,
  clearUserState,
  selectToken,
  selectUserInfo,
  selectMenuTree,
  selectDisplayMenuTree,
  selectPointsList,
  selectMenuRouterList,
  selectMenuParentIdListMap,
  selectAdministratorFlag,
  selectUserLoading,
  selectUserError,
} from './userSlice';
import { loginApi, LoginForm } from '@/api/system/loginApi';
import { LOCAL_STORAGE_KEYS } from '@/constants/storageKeys';
import type { RootState } from '../index';
import type { MenuItem, PermissionPoint } from '@/types/menu';

// ==================== Mock 設置 ====================

vi.mock('@/api/system/loginApi', () => ({
  loginApi: {
    login: vi.fn(),
    getLoginInfo: vi.fn(),
    logout: vi.fn(),
  },
}));

// ==================== 測試數據 ====================

const mockLoginForm: LoginForm = {
  loginName: 'admin',
  password: 'password123',
  captchaCode: '1234',
  captchaUuid: 'uuid-123',
};

const mockLoginResult = {
  token: 'mock-jwt-token-12345',
  employeeId: '1001',
};

const mockMenuItems: MenuItem[] = [
  {
    menuId: 1,
    menuName: '系統管理',
    menuType: 1,
    path: '/system',
    parentId: 0,
    sort: 1,
    perms: '',
    visibleFlag: true,
    disabledFlag: false,
    children: [
      {
        menuId: 2,
        menuName: '用戶管理',
        menuType: 2,
        path: '/system/user',
        parentId: 1,
        sort: 1,
        perms: 'system:user:query',
        visibleFlag: true,
        disabledFlag: false,
        children: [],
      },
      {
        menuId: 3,
        menuName: '角色管理',
        menuType: 2,
        path: '/system/role',
        parentId: 1,
        sort: 2,
        perms: 'system:role:query',
        visibleFlag: true,
        disabledFlag: false,
        children: [],
      },
    ],
  },
  {
    menuId: 4,
    menuName: '業務管理',
    menuType: 1,
    path: '/business',
    parentId: 0,
    sort: 2,
    perms: '',
    visibleFlag: true,
    disabledFlag: false,
    children: [
      {
        menuId: 5,
        menuName: '商品管理',
        menuType: 2,
        path: '/business/goods',
        parentId: 4,
        sort: 1,
        perms: 'business:goods:query',
        visibleFlag: true,
        disabledFlag: false,
        children: [],
      },
    ],
  },
];

const mockPermissionPoints: PermissionPoint[] = [
  { menuId: 101, webPerms: 'system:user:add', menuName: '新增用戶' },
  { menuId: 102, webPerms: 'system:user:edit', menuName: '編輯用戶' },
  { menuId: 103, webPerms: 'system:user:delete', menuName: '刪除用戶' },
];

const mockLoginInfo = {
  employeeId: '1001',
  employeeName: 'Admin User',
  loginName: 'admin',
  administratorFlag: true,
  menuTreeList: [
    {
      menuId: 1,
      menuName: '系統管理',
      menuType: 1,
      path: '/system',
      parentId: 0,
      sort: 1,
      perms: '',
      children: [
        {
          menuId: 2,
          menuName: '用戶管理',
          menuType: 2,
          path: '/system/user',
          parentId: 1,
          sort: 1,
          perms: 'system:user:query',
          children: [],
        },
      ],
    },
  ],
  pointList: [
    { menuId: 201, webPerms: 'system:user:add', menuName: '新增用戶' },
    { menuId: 202, webPerms: 'system:user:edit', menuName: '編輯用戶' },
  ],
};

describe('userSlice', () => {
  let originalLocalStorage: Storage;

  beforeEach(() => {
    // 保存原始 localStorage
    originalLocalStorage = global.localStorage;

    // Mock localStorage
    const localStorageMock = (() => {
      let store: Record<string, string> = {};
      return {
        getItem: vi.fn((key: string) => store[key] || null),
        setItem: vi.fn((key: string, value: string) => {
          store[key] = value;
        }),
        removeItem: vi.fn((key: string) => {
          delete store[key];
        }),
        clear: vi.fn(() => {
          store = {};
        }),
        get length() {
          return Object.keys(store).length;
        },
        key: vi.fn((index: number) => Object.keys(store)[index] || null),
      };
    })();
    global.localStorage = localStorageMock as Storage;
  });

  afterEach(() => {
    // 恢復原始 localStorage
    global.localStorage = originalLocalStorage;
    vi.clearAllMocks();
  });

  // ==================== 初始狀態測試 ====================

  describe('初始狀態', () => {
    it('應該返回默認狀態', () => {
      const state = userReducer(undefined, { type: '' });
      expect(state.token).toBe('');
      expect(state.employeeId).toBe('');
      expect(state.employeeName).toBe('');
      expect(state.loginName).toBe('');
      expect(state.administratorFlag).toBe(false);
      expect(state.menuTree).toEqual([]);
      expect(state.displayMenuTree).toEqual([]);
      expect(state.pointsList).toEqual([]);
      expect(state.menuRouterList).toEqual([]);
      expect(state.menuParentIdListMap).toEqual({});
      expect(state.loading).toBe(false);
      expect(state.error).toBe(null);
    });
  });

  // ==================== Reducers 測試 ====================

  describe('Reducers', () => {
    it('setToken - 應該設置 Token 並同步 localStorage', () => {
      const state = userReducer(undefined, setToken('new-token-123'));

      expect(state.token).toBe('new-token-123');
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        LOCAL_STORAGE_KEYS.USER_TOKEN,
        'new-token-123'
      );
    });

    it('setUserInfo - 應該批量更新用戶信息', () => {
      const state = userReducer(
        undefined,
        setUserInfo({
          employeeId: '2001',
          employeeName: 'Test User',
          loginName: 'testuser',
          administratorFlag: false,
        })
      );

      expect(state.employeeId).toBe('2001');
      expect(state.employeeName).toBe('Test User');
      expect(state.loginName).toBe('testuser');
      expect(state.administratorFlag).toBe(false);
    });

    it('clearUserState - 應該清除所有狀態並移除 localStorage', () => {
      // 先設置一些狀態
      let state = userReducer(undefined, setToken('token-to-clear'));
      state = userReducer(
        state,
        setUserInfo({
          employeeId: '3001',
          employeeName: 'User to Clear',
        })
      );

      // 清除狀態
      state = userReducer(state, clearUserState());

      expect(state.token).toBe('');
      expect(state.employeeId).toBe('');
      expect(state.employeeName).toBe('');
      expect(state.loginName).toBe('');
      expect(state.administratorFlag).toBe(false);
      expect(state.menuTree).toEqual([]);
      expect(state.displayMenuTree).toEqual([]);
      expect(state.pointsList).toEqual([]);
      expect(state.menuRouterList).toEqual([]);
      expect(state.menuParentIdListMap).toEqual({});
      expect(state.error).toBe(null);

      expect(global.localStorage.removeItem).toHaveBeenCalledWith(LOCAL_STORAGE_KEYS.USER_TOKEN);
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(LOCAL_STORAGE_KEYS.USER_INFO);
    });
  });

  // ==================== login AsyncThunk 測試 ====================

  describe('login AsyncThunk', () => {
    it('pending - 應該設置 loading=true, error=null', () => {
      const state = userReducer(undefined, { type: login.pending.type });

      expect(state.loading).toBe(true);
      expect(state.error).toBe(null);
    });

    it('fulfilled - 應該保存 token, employeeId 到 State 和 localStorage', () => {
      const action = {
        type: login.fulfilled.type,
        payload: mockLoginResult,
      };
      const state = userReducer(undefined, action);

      expect(state.loading).toBe(false);
      expect(state.token).toBe('mock-jwt-token-12345');
      expect(state.employeeId).toBe('1001');
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        LOCAL_STORAGE_KEYS.USER_TOKEN,
        'mock-jwt-token-12345'
      );
    });

    it('rejected - 應該設置 error 並保持 loading=false', () => {
      const action = {
        type: login.rejected.type,
        payload: '登錄失敗：用戶名或密碼錯誤',
      };
      const state = userReducer(undefined, action);

      expect(state.loading).toBe(false);
      expect(state.error).toBe('登錄失敗：用戶名或密碼錯誤');
    });

    it('API 返回 ok=false - 應該調用 rejectWithValue', async () => {
      (loginApi.login as any).mockResolvedValue({
        ok: false,
        msg: 'API 錯誤',
      });

      const result = await login(mockLoginForm)(
        vi.fn(),
        vi.fn(),
        {}
      );

      expect(result.type).toBe(login.rejected.type);
    });
  });

  // ==================== getLoginInfo AsyncThunk 測試 ====================

  describe('getLoginInfo AsyncThunk', () => {
    it('pending - 應該設置 loading=true', () => {
      const state = userReducer(undefined, { type: getLoginInfo.pending.type });

      expect(state.loading).toBe(true);
      expect(state.error).toBe(null);
    });

    it('fulfilled - 應該處理完整的菜單樹數據', async () => {
      (loginApi.getLoginInfo as any).mockResolvedValue({
        ok: true,
        data: mockLoginInfo,
      });

      const action = await getLoginInfo()(vi.fn(), vi.fn(), {});

      expect(action.type).toBe(getLoginInfo.fulfilled.type);
      expect(action.payload).toHaveProperty('menuTree');
      expect(action.payload).toHaveProperty('displayMenuTree');
      expect(action.payload).toHaveProperty('pointsList');
      expect(action.payload).toHaveProperty('menuRouterList');
      expect(action.payload).toHaveProperty('menuParentIdListMap');
    });

    it('fulfilled - 應該正確合併菜單權限點和獨立權限點', async () => {
      (loginApi.getLoginInfo as any).mockResolvedValue({
        ok: true,
        data: mockLoginInfo,
      });

      const action = await getLoginInfo()(vi.fn(), vi.fn(), {});

      // pointsList 應該包含菜單權限點 + 獨立權限點
      expect(action.payload.pointsList).toBeDefined();
      expect(Array.isArray(action.payload.pointsList)).toBe(true);
      expect(action.payload.pointsList.length).toBeGreaterThan(0);
    });

    it('rejected - 應該設置 error', async () => {
      (loginApi.getLoginInfo as any).mockRejectedValue(new Error('網絡錯誤'));

      const action = await getLoginInfo()(vi.fn(), vi.fn(), {});

      expect(action.type).toBe(getLoginInfo.rejected.type);
      expect(action.payload).toBe('網絡錯誤');
    });
  });

  // ==================== logout AsyncThunk 測試 ====================

  describe('logout AsyncThunk', () => {
    it('pending - 應該設置 loading=true', () => {
      const state = userReducer(undefined, { type: logout.pending.type });

      expect(state.loading).toBe(true);
      expect(state.error).toBe(null);
    });

    it('fulfilled - 應該清除所有狀態和 localStorage', () => {
      // 先設置一些狀態
      let state = userReducer(undefined, setToken('token-to-logout'));
      state = userReducer(
        state,
        setUserInfo({
          employeeId: '4001',
          employeeName: 'User to Logout',
        })
      );

      // 退出登錄
      state = userReducer(state, { type: logout.fulfilled.type });

      expect(state.loading).toBe(false);
      expect(state.token).toBe('');
      expect(state.employeeId).toBe('');
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(LOCAL_STORAGE_KEYS.USER_TOKEN);
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(LOCAL_STORAGE_KEYS.USER_INFO);
    });

    it('rejected - 應該設置 error 但仍清除本地狀態', () => {
      const action = {
        type: logout.rejected.type,
        payload: '退出登錄失敗',
      };
      const state = userReducer(undefined, action);

      expect(state.loading).toBe(false);
      expect(state.error).toBe('退出登錄失敗');
    });

    it('rejected - 應該移除 localStorage 即使退出失敗', () => {
      // 先設置狀態
      let state = userReducer(undefined, setToken('token-logout-fail'));

      // 退出失敗，但仍應清除本地狀態
      state = userReducer(state, { type: logout.fulfilled.type });

      expect(global.localStorage.removeItem).toHaveBeenCalledWith(LOCAL_STORAGE_KEYS.USER_TOKEN);
    });
  });

  // ==================== Selectors 測試 ====================

  describe('Selectors', () => {
    const mockState: Partial<RootState> = {
      user: {
        token: 'selector-token-123',
        employeeId: '5001',
        employeeName: 'Selector User',
        loginName: 'selectoruser',
        administratorFlag: true,
        menuTree: mockMenuItems,
        displayMenuTree: mockMenuItems,
        pointsList: mockPermissionPoints,
        menuRouterList: ['/system', '/system/user', '/system/role'],
        menuParentIdListMap: {
          1: [0],
          2: [0, 1],
          3: [0, 1],
        },
        loading: false,
        error: null,
      },
    };

    it('selectToken', () => {
      expect(selectToken(mockState as RootState)).toBe('selector-token-123');
    });

    it('selectUserInfo', () => {
      const userInfo = selectUserInfo(mockState as RootState);
      expect(userInfo.employeeId).toBe('5001');
      expect(userInfo.employeeName).toBe('Selector User');
      expect(userInfo.loginName).toBe('selectoruser');
      expect(userInfo.administratorFlag).toBe(true);
    });

    it('selectMenuTree', () => {
      expect(selectMenuTree(mockState as RootState)).toEqual(mockMenuItems);
    });

    it('selectDisplayMenuTree', () => {
      expect(selectDisplayMenuTree(mockState as RootState)).toEqual(mockMenuItems);
    });

    it('selectPointsList', () => {
      expect(selectPointsList(mockState as RootState)).toEqual(mockPermissionPoints);
    });

    it('selectMenuRouterList', () => {
      expect(selectMenuRouterList(mockState as RootState)).toEqual([
        '/system',
        '/system/user',
        '/system/role',
      ]);
    });

    it('selectMenuParentIdListMap', () => {
      expect(selectMenuParentIdListMap(mockState as RootState)).toEqual({
        1: [0],
        2: [0, 1],
        3: [0, 1],
      });
    });

    it('selectAdministratorFlag', () => {
      expect(selectAdministratorFlag(mockState as RootState)).toBe(true);
    });

    it('selectUserLoading', () => {
      expect(selectUserLoading(mockState as RootState)).toBe(false);
    });

    it('selectUserError', () => {
      expect(selectUserError(mockState as RootState)).toBe(null);
    });
  });

  // ==================== 完整工作流測試 ====================

  describe('完整工作流', () => {
    it('應該支持完整的登錄流程：login → getLoginInfo → logout', async () => {
      // Step 1: 登錄
      (loginApi.login as any).mockResolvedValue({
        ok: true,
        data: mockLoginResult,
      });

      let state = userReducer(undefined, {
        type: login.fulfilled.type,
        payload: mockLoginResult,
      });

      expect(state.token).toBe('mock-jwt-token-12345');
      expect(state.employeeId).toBe('1001');

      // Step 2: 獲取登錄信息
      (loginApi.getLoginInfo as any).mockResolvedValue({
        ok: true,
        data: mockLoginInfo,
      });

      const loginInfoAction = await getLoginInfo()(vi.fn(), vi.fn(), {});
      state = userReducer(state, loginInfoAction);

      expect(state.employeeName).toBe('Admin User');
      expect(state.loginName).toBe('admin');
      expect(state.administratorFlag).toBe(true);

      // Step 3: 退出登錄
      (loginApi.logout as any).mockResolvedValue({
        ok: true,
      });

      state = userReducer(state, { type: logout.fulfilled.type });

      expect(state.token).toBe('');
      expect(state.employeeId).toBe('');
      expect(state.employeeName).toBe('');
    });
  });

  // ==================== 邊界情況測試 ====================

  describe('邊界情況', () => {
    it('應該處理 API 網絡錯誤', async () => {
      (loginApi.login as any).mockRejectedValue(new Error('網絡連接失敗'));

      const action = await login(mockLoginForm)(vi.fn(), vi.fn(), {});

      expect(action.type).toBe(login.rejected.type);
      expect(action.payload).toBe('網絡連接失敗');
    });

    it('應該處理空菜單樹數據', async () => {
      (loginApi.getLoginInfo as any).mockResolvedValue({
        ok: true,
        data: {
          ...mockLoginInfo,
          menuTreeList: [],
          pointList: [],
        },
      });

      const action = await getLoginInfo()(vi.fn(), vi.fn(), {});

      expect(action.type).toBe(getLoginInfo.fulfilled.type);
      expect(action.payload.menuTree).toEqual([]);
      expect(action.payload.pointsList).toEqual([]);
    });

    it('應該保持狀態不可變性', () => {
      const initialState = userReducer(undefined, { type: '' });
      const newState = userReducer(initialState, setToken('immutable-token'));

      // 原始狀態不應改變
      expect(initialState.token).toBe('');
      expect(newState.token).toBe('immutable-token');
      expect(initialState).not.toBe(newState);
    });
  });
});
