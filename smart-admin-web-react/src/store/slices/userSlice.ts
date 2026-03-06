/**
 * 用戶狀態管理（對應 Vue Pinia userStore）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { UserState, LoginResult, MenuPoint } from '@/types/user.types';

/**
 * 初始狀態
 */
const initialState: UserState = {
  token: '',
  employeeId: '',
  employeeName: '',
  administratorFlag: false,
  pointsList: [],
  menuTree: [],
  departmentId: '',
  departmentName: '',
};

/**
 * 用戶 Slice（對應 Vue 的 useUserStore）
 */
export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    /**
     * 設置用戶登錄信息（對應 Vue setUserLoginInfo）
     */
    setUserLoginInfo: (state, action: PayloadAction<LoginResult>) => {
      const { token, employeeId, employeeName, administratorFlag, menuList } =
        action.payload;

      state.token = token;
      state.employeeId = employeeId;
      state.employeeName = employeeName;
      state.administratorFlag = administratorFlag;

      // 過濾功能點（對應 Vue 的過濾邏輯）
      // 只保留 menuType === 'POINTS' 且可見且未禁用的記錄
      state.pointsList = menuList.filter(
        (menu) =>
          'menuType' in menu &&
          menu.menuType === 'POINTS' &&
          menu.visibleFlag &&
          !menu.disabledFlag
      ) as MenuPoint[];

      // 過濾菜單樹（目錄和菜單，不包含功能點）
      state.menuTree = menuList.filter(
        (menu) =>
          'menuType' in menu &&
          (menu.menuType === 'CATALOG' || menu.menuType === 'MENU') &&
          menu.visibleFlag &&
          !menu.disabledFlag
      );
    },

    /**
     * 設置 Token
     */
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload;
    },

    /**
     * 設置權限列表（用於測試或特殊場景）
     */
    setPointsList: (state, action: PayloadAction<MenuPoint[]>) => {
      state.pointsList = action.payload;
    },

    /**
     * 設置管理員標識（用於測試或特殊場景）
     */
    setAdministratorFlag: (state, action: PayloadAction<boolean>) => {
      state.administratorFlag = action.payload;
    },

    /**
     * 登出（對應 Vue logout）
     */
    logout: (state) => {
      state.token = '';
      state.employeeId = '';
      state.employeeName = '';
      state.administratorFlag = false;
      state.pointsList = [];
      state.menuTree = [];
      state.departmentId = '';
      state.departmentName = '';
    },
  },
});

// ================================= Selectors =================================

/**
 * 選擇器：獲取 Token
 */
export const selectToken = (state: { user: UserState }) => state.user.token;

/**
 * 選擇器：獲取員工 ID
 */
export const selectEmployeeId = (state: { user: UserState }) =>
  state.user.employeeId;

/**
 * 選擇器：獲取員工名稱
 */
export const selectEmployeeName = (state: { user: UserState }) =>
  state.user.employeeName;

/**
 * 選擇器：獲取管理員標識（對應 Vue administratorFlag）
 */
export const selectAdministratorFlag = (state: { user: UserState }) =>
  state.user.administratorFlag;

/**
 * 選擇器：獲取權限點列表（對應 Vue getPointList）
 */
export const selectPointsList = (state: { user: UserState }) =>
  state.user.pointsList;

/**
 * 選擇器：獲取菜單樹
 */
export const selectMenuTree = (state: { user: UserState }) =>
  state.user.menuTree;

/**
 * 選擇器：檢查是否已登錄
 */
export const selectIsLoggedIn = (state: { user: UserState }) =>
  !!state.user.token;

// ================================= Actions =================================

export const {
  setUserLoginInfo,
  setToken,
  setPointsList,
  setAdministratorFlag,
  logout,
} = userSlice.actions;

// ================================= Reducer =================================

export default userSlice.reducer;
