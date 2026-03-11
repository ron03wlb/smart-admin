/**
 * Spin Slice 單元測試
 * 全局加載狀態管理測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect } from 'vitest';
import spinReducer, { showLoading, hideLoading, selectLoading } from './spinSlice';
import type { SpinState } from './spinSlice';
import type { RootState } from '../index';

describe('spinSlice', () => {
  // ==================== 初始狀態測試 ====================

  it('should return initial state', () => {
    const initialState: SpinState = {
      loading: false,
    };

    expect(spinReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  // ==================== showLoading Action 測試 ====================

  it('should handle showLoading', () => {
    const initialState: SpinState = {
      loading: false,
    };

    const state = spinReducer(initialState, showLoading());

    expect(state.loading).toBe(true);
  });

  it('should handle showLoading when already loading', () => {
    const initialState: SpinState = {
      loading: true,
    };

    const state = spinReducer(initialState, showLoading());

    // 應該保持為 true
    expect(state.loading).toBe(true);
  });

  // ==================== hideLoading Action 測試 ====================

  it('should handle hideLoading', () => {
    const initialState: SpinState = {
      loading: true,
    };

    const state = spinReducer(initialState, hideLoading());

    expect(state.loading).toBe(false);
  });

  it('should handle hideLoading when already not loading', () => {
    const initialState: SpinState = {
      loading: false,
    };

    const state = spinReducer(initialState, hideLoading());

    // 應該保持為 false
    expect(state.loading).toBe(false);
  });

  // ==================== Loading 狀態切換測試 ====================

  it('should toggle loading state correctly', () => {
    let state: SpinState = {
      loading: false,
    };

    // 顯示 loading
    state = spinReducer(state, showLoading());
    expect(state.loading).toBe(true);

    // 隱藏 loading
    state = spinReducer(state, hideLoading());
    expect(state.loading).toBe(false);

    // 再次顯示
    state = spinReducer(state, showLoading());
    expect(state.loading).toBe(true);
  });

  // ==================== Selector 測試 ====================

  it('should select loading state correctly', () => {
    const mockState: RootState = {
      spin: { loading: true },
    } as RootState;

    expect(selectLoading(mockState)).toBe(true);
  });

  it('should select loading state as false', () => {
    const mockState: RootState = {
      spin: { loading: false },
    } as RootState;

    expect(selectLoading(mockState)).toBe(false);
  });

  // ==================== 邊界情況測試 ====================

  it('should handle multiple consecutive showLoading calls', () => {
    let state: SpinState = {
      loading: false,
    };

    // 連續多次調用 showLoading
    state = spinReducer(state, showLoading());
    state = spinReducer(state, showLoading());
    state = spinReducer(state, showLoading());

    // 狀態應該保持為 true
    expect(state.loading).toBe(true);
  });

  it('should handle multiple consecutive hideLoading calls', () => {
    let state: SpinState = {
      loading: true,
    };

    // 連續多次調用 hideLoading
    state = spinReducer(state, hideLoading());
    state = spinReducer(state, hideLoading());
    state = spinReducer(state, hideLoading());

    // 狀態應該保持為 false
    expect(state.loading).toBe(false);
  });

  // ==================== 狀態不可變性測試 ====================

  it('should not mutate the original state', () => {
    const initialState: SpinState = {
      loading: false,
    };

    const originalState = { ...initialState };

    spinReducer(initialState, showLoading());

    // 原始狀態不應改變
    expect(initialState).toEqual(originalState);
  });
});
