/**
 * 菜單狀態管理
 *
 * 功能：
 * 1. 管理側邊菜單折疊/展開狀態
 * 2. 管理當前選中的菜單項
 * 3. 管理展開的子菜單列表
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

/**
 * 菜單狀態
 */
export interface MenuState {
  /**
   * 側邊菜單是否折疊
   */
  collapsed: boolean;

  /**
   * 當前選中的菜單 ID
   */
  selectedMenuId: string;

  /**
   * 展開的子菜單 ID 列表
   */
  openKeys: string[];
}

/**
 * 初始狀態
 */
const initialState: MenuState = {
  collapsed: false,
  selectedMenuId: '',
  openKeys: [],
};

/**
 * 菜單 Slice
 */
export const menuSlice = createSlice({
  name: 'menu',
  initialState,
  reducers: {
    /**
     * 切換菜單折疊狀態
     */
    toggleCollapsed: (state) => {
      state.collapsed = !state.collapsed;
      // 折疊時關閉所有子菜單
      if (state.collapsed) {
        state.openKeys = [];
      }
    },

    /**
     * 設置菜單折疊狀態
     */
    setCollapsed: (state, action: PayloadAction<boolean>) => {
      state.collapsed = action.payload;
      // 折疊時關閉所有子菜單
      if (state.collapsed) {
        state.openKeys = [];
      }
    },

    /**
     * 設置當前選中的菜單 ID
     */
    setSelectedMenuId: (state, action: PayloadAction<string>) => {
      state.selectedMenuId = action.payload;
    },

    /**
     * 設置展開的子菜單 ID 列表
     */
    setOpenKeys: (state, action: PayloadAction<string[]>) => {
      state.openKeys = action.payload;
    },

    /**
     * 重置菜單狀態
     */
    resetMenuState: (state) => {
      state.collapsed = false;
      state.selectedMenuId = '';
      state.openKeys = [];
    },
  },
});

// ================================= Selectors =================================

/**
 * 選擇器：獲取菜單折疊狀態
 */
export const selectCollapsed = (state: { menu: MenuState }) =>
  state.menu.collapsed;

/**
 * 選擇器：獲取當前選中的菜單 ID
 */
export const selectSelectedMenuId = (state: { menu: MenuState }) =>
  state.menu.selectedMenuId;

/**
 * 選擇器：獲取展開的子菜單 ID 列表
 */
export const selectOpenKeys = (state: { menu: MenuState }) =>
  state.menu.openKeys;

// ================================= Actions =================================

export const {
  toggleCollapsed,
  setCollapsed,
  setSelectedMenuId,
  setOpenKeys,
  resetMenuState,
} = menuSlice.actions;

// ================================= Reducer =================================

export default menuSlice.reducer;
