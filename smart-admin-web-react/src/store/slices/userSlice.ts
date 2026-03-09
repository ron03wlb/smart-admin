/**
 * User Slice
 * 用戶狀態管理（登錄、菜單、權限等）
 *
 * 參考：Vue 版本 smart-admin-web/src/store/modules/system/user.ts (365 行)
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { loginApi, LoginForm } from '@/api/system/loginApi';
import type { RootState } from '../index';
import { LOCAL_STORAGE_KEYS } from '@/constants/storageKeys';

// ==================== State 類型定義 ====================

interface UserState {
  /** JWT Token */
  token: string;

  /** 員工ID */
  employeeId: string;

  /** 員工姓名 */
  employeeName: string;

  /** 登錄帳號 */
  loginName: string;

  /** 是否管理員 */
  administratorFlag: boolean;

  /** 菜單樹 */
  menuTree: any[];

  /** 權限點列表 */
  pointsList: any[];

  /** 加載狀態 */
  loading: boolean;

  /** 錯誤信息 */
  error: string | null;
}

// ==================== 初始狀態 ====================

const initialState: UserState = {
  token: '',
  employeeId: '',
  employeeName: '',
  loginName: '',
  administratorFlag: false,
  menuTree: [],
  pointsList: [],
  loading: false,
  error: null,
};

// ==================== Async Thunks ====================

/**
 * 登錄 AsyncThunk
 * @param loginForm 登錄表單
 */
export const login = createAsyncThunk(
  'user/login',
  async (loginForm: LoginForm, { rejectWithValue }) => {
    try {
      const response = await loginApi.login(loginForm);

      if (response.ok && response.data) {
        return response.data;
      } else {
        return rejectWithValue(response.msg || '登錄失敗');
      }
    } catch (error: any) {
      return rejectWithValue(error.message || '網絡錯誤');
    }
  }
);

/**
 * 獲取登錄信息 AsyncThunk
 * 包含菜單、權限等信息
 */
export const getLoginInfo = createAsyncThunk(
  'user/getLoginInfo',
  async (_, { rejectWithValue }) => {
    try {
      const response = await loginApi.getLoginInfo();

      if (response.ok && response.data) {
        return response.data;
      } else {
        return rejectWithValue(response.msg || '獲取登錄信息失敗');
      }
    } catch (error: any) {
      return rejectWithValue(error.message || '網絡錯誤');
    }
  }
);

/**
 * 退出登錄 AsyncThunk
 */
export const logout = createAsyncThunk(
  'user/logout',
  async (_, { rejectWithValue }) => {
    try {
      const response = await loginApi.logout();

      if (response.ok) {
        return;
      } else {
        return rejectWithValue(response.msg || '退出登錄失敗');
      }
    } catch (error: any) {
      return rejectWithValue(error.message || '網絡錯誤');
    }
  }
);

// ==================== Slice ====================

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    /**
     * 設置 Token
     */
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload;
      localStorage.setItem(LOCAL_STORAGE_KEYS.USER_TOKEN, action.payload);
    },

    /**
     * 設置用戶信息
     */
    setUserInfo: (state, action: PayloadAction<Partial<UserState>>) => {
      Object.assign(state, action.payload);
    },

    /**
     * 清除用戶狀態（本地退出）
     */
    clearUserState: (state) => {
      state.token = '';
      state.employeeId = '';
      state.employeeName = '';
      state.loginName = '';
      state.administratorFlag = false;
      state.menuTree = [];
      state.pointsList = [];
      state.error = null;

      localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_TOKEN);
      localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_INFO);
    },
  },
  extraReducers: (builder) => {
    // ========== login ==========
    builder
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.token = action.payload.token;
        state.employeeId = action.payload.employeeId;

        // 存儲 Token 到 localStorage
        localStorage.setItem(LOCAL_STORAGE_KEYS.USER_TOKEN, action.payload.token);
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string || '登錄失敗';
      });

    // ========== getLoginInfo ==========
    builder
      .addCase(getLoginInfo.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getLoginInfo.fulfilled, (state, action) => {
        state.loading = false;
        state.employeeId = action.payload.employeeId;
        state.employeeName = action.payload.employeeName;
        state.loginName = action.payload.loginName;
        state.administratorFlag = action.payload.administratorFlag;
        state.menuTree = action.payload.menuTreeList || [];
        state.pointsList = action.payload.pointList || [];

        // 存儲用戶信息到 localStorage
        localStorage.setItem(LOCAL_STORAGE_KEYS.USER_INFO, JSON.stringify({
          employeeId: action.payload.employeeId,
          employeeName: action.payload.employeeName,
          loginName: action.payload.loginName,
          administratorFlag: action.payload.administratorFlag,
        }));
      })
      .addCase(getLoginInfo.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string || '獲取登錄信息失敗';
      });

    // ========== logout ==========
    builder
      .addCase(logout.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(logout.fulfilled, (state) => {
        // 清除所有狀態
        state.loading = false;
        state.token = '';
        state.employeeId = '';
        state.employeeName = '';
        state.loginName = '';
        state.administratorFlag = false;
        state.menuTree = [];
        state.pointsList = [];

        // 清除 localStorage
        localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_TOKEN);
        localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_INFO);
      })
      .addCase(logout.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string || '退出登錄失敗';

        // 即使退出失敗，也清除本地狀態
        state.token = '';
        state.employeeId = '';
        state.menuTree = [];
        state.pointsList = [];
        localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_TOKEN);
        localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_INFO);
      });
  },
});

// ==================== Selectors ====================

export const selectToken = (state: RootState) => state.user.token;
export const selectUserInfo = (state: RootState) => ({
  employeeId: state.user.employeeId,
  employeeName: state.user.employeeName,
  loginName: state.user.loginName,
  administratorFlag: state.user.administratorFlag,
});
export const selectMenuTree = (state: RootState) => state.user.menuTree;
export const selectPointsList = (state: RootState) => state.user.pointsList;
export const selectAdministratorFlag = (state: RootState) => state.user.administratorFlag;
export const selectUserLoading = (state: RootState) => state.user.loading;
export const selectUserError = (state: RootState) => state.user.error;

// ==================== Actions ====================

export const { setToken, setUserInfo, clearUserState } = userSlice.actions;

// ==================== Reducer ====================

export default userSlice.reducer;
