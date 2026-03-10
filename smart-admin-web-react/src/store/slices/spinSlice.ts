/**
 * Spin Slice
 * 全局加載狀態管理
 *
 * 參考：Vue 版本 smart-admin-web/src/store/modules/system/spin.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { createSlice } from '@reduxjs/toolkit';
import type { RootState } from '../index';

export interface SpinState {
  /**
   * 全局加載狀態
   */
  loading: boolean;
}

const initialState: SpinState = {
  loading: false,
};

const spinSlice = createSlice({
  name: 'spin',
  initialState,
  reducers: {
    /**
     * 顯示全局 loading
     */
    showLoading: state => {
      state.loading = true;
    },

    /**
     * 隱藏全局 loading
     */
    hideLoading: state => {
      state.loading = false;
    },
  },
});

export const { showLoading, hideLoading } = spinSlice.actions;

/**
 * Selector: 獲取 loading 狀態
 */
export const selectLoading = (state: RootState) => state.spin.loading;

export default spinSlice.reducer;
