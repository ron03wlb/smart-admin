/**
 * menuSlice Redux Slice 測試
 *
 * 測試覆蓋：
 * 1. 初始狀態正確
 * 2. toggleCollapsed 切換折疊狀態
 * 3. toggleCollapsed 折疊時自動清空 openKeys
 * 4. setCollapsed 設置折疊狀態
 * 5. setCollapsed 折疊時自動清空 openKeys
 * 6. setSelectedMenuId 設置選中菜單
 * 7. setOpenKeys 設置展開的子菜單
 * 8. resetMenuState 重置菜單狀態
 * 9. selectCollapsed 選擇器正確返回 collapsed
 * 10. selectSelectedMenuId 選擇器正確返回 selectedMenuId
 * 11. selectOpenKeys 選擇器正確返回 openKeys
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { describe, test, expect } from 'vitest';
import menuReducer, {
  toggleCollapsed,
  setCollapsed,
  setSelectedMenuId,
  setOpenKeys,
  resetMenuState,
  selectCollapsed,
  selectSelectedMenuId,
  selectOpenKeys,
} from '../menuSlice';
import type { MenuState } from '../menuSlice';

describe('menuSlice', () => {
  /**
   * 測試 1：初始狀態應該正確
   */
  test('初始狀態應該正確', () => {
    const initialState = menuReducer(undefined, { type: 'unknown' });

    expect(initialState).toEqual({
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    });
  });

  /**
   * 測試 2：toggleCollapsed 應該切換折疊狀態
   */
  test('toggleCollapsed 應該切換折疊狀態', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    };

    // 1. 第一次切換：false → true
    const state1 = menuReducer(initialState, toggleCollapsed());
    expect(state1.collapsed).toBe(true);

    // 2. 第二次切換：true → false
    const state2 = menuReducer(state1, toggleCollapsed());
    expect(state2.collapsed).toBe(false);
  });

  /**
   * 測試 3：toggleCollapsed 折疊時應該自動清空 openKeys
   */
  test('toggleCollapsed 折疊時應該自動清空 openKeys', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: ['1', '2', '3'],
    };

    // 1. 切換到折疊狀態
    const state1 = menuReducer(initialState, toggleCollapsed());
    expect(state1.collapsed).toBe(true);
    expect(state1.openKeys).toEqual([]);

    // 2. 切換回展開狀態（openKeys 仍為空）
    const state2 = menuReducer(state1, toggleCollapsed());
    expect(state2.collapsed).toBe(false);
    expect(state2.openKeys).toEqual([]);
  });

  /**
   * 測試 4：setCollapsed 應該設置折疊狀態
   */
  test('setCollapsed 應該設置折疊狀態', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    };

    // 1. 設置為 true
    const state1 = menuReducer(initialState, setCollapsed(true));
    expect(state1.collapsed).toBe(true);

    // 2. 設置為 false
    const state2 = menuReducer(state1, setCollapsed(false));
    expect(state2.collapsed).toBe(false);
  });

  /**
   * 測試 5：setCollapsed 折疊時應該自動清空 openKeys
   */
  test('setCollapsed 折疊時應該自動清空 openKeys', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: ['1', '2'],
    };

    // 1. 設置為折疊狀態
    const state1 = menuReducer(initialState, setCollapsed(true));
    expect(state1.collapsed).toBe(true);
    expect(state1.openKeys).toEqual([]);

    // 2. 設置為展開狀態（openKeys 不會自動恢復）
    const state2 = menuReducer(state1, setCollapsed(false));
    expect(state2.collapsed).toBe(false);
    expect(state2.openKeys).toEqual([]);
  });

  /**
   * 測試 6：setSelectedMenuId 應該設置選中菜單 ID
   */
  test('setSelectedMenuId 應該設置選中菜單 ID', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    };

    // 1. 設置選中菜單 ID 為 '11'
    const state1 = menuReducer(initialState, setSelectedMenuId('11'));
    expect(state1.selectedMenuId).toBe('11');

    // 2. 更新選中菜單 ID 為 '22'
    const state2 = menuReducer(state1, setSelectedMenuId('22'));
    expect(state2.selectedMenuId).toBe('22');

    // 3. 清空選中菜單 ID
    const state3 = menuReducer(state2, setSelectedMenuId(''));
    expect(state3.selectedMenuId).toBe('');
  });

  /**
   * 測試 7：setOpenKeys 應該設置展開的子菜單 keys
   */
  test('setOpenKeys 應該設置展開的子菜單 keys', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    };

    // 1. 設置 openKeys 為 ['1']
    const state1 = menuReducer(initialState, setOpenKeys(['1']));
    expect(state1.openKeys).toEqual(['1']);

    // 2. 更新 openKeys 為 ['1', '2']
    const state2 = menuReducer(state1, setOpenKeys(['1', '2']));
    expect(state2.openKeys).toEqual(['1', '2']);

    // 3. 清空 openKeys
    const state3 = menuReducer(state2, setOpenKeys([]));
    expect(state3.openKeys).toEqual([]);
  });

  /**
   * 測試 8：resetMenuState 應該重置菜單狀態
   */
  test('resetMenuState 應該重置菜單狀態', () => {
    const initialState: MenuState = {
      collapsed: true,
      selectedMenuId: '11',
      openKeys: ['1', '2'],
    };

    const state = menuReducer(initialState, resetMenuState());

    expect(state).toEqual({
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    });
  });

  /**
   * 測試 9：selectCollapsed 選擇器應該正確返回 collapsed 狀態
   */
  test('selectCollapsed 選擇器應該正確返回 collapsed 狀態', () => {
    const state = {
      menu: {
        collapsed: true,
        selectedMenuId: '',
        openKeys: [],
      },
    };

    expect(selectCollapsed(state)).toBe(true);
  });

  /**
   * 測試 10：selectSelectedMenuId 選擇器應該正確返回 selectedMenuId
   */
  test('selectSelectedMenuId 選擇器應該正確返回 selectedMenuId', () => {
    const state = {
      menu: {
        collapsed: false,
        selectedMenuId: '11',
        openKeys: [],
      },
    };

    expect(selectSelectedMenuId(state)).toBe('11');
  });

  /**
   * 測試 11：selectOpenKeys 選擇器應該正確返回 openKeys
   */
  test('selectOpenKeys 選擇器應該正確返回 openKeys', () => {
    const state = {
      menu: {
        collapsed: false,
        selectedMenuId: '',
        openKeys: ['1', '2', '3'],
      },
    };

    expect(selectOpenKeys(state)).toEqual(['1', '2', '3']);
  });

  /**
   * 測試 12：多個 action 組合應該正確更新狀態
   */
  test('多個 action 組合應該正確更新狀態', () => {
    const initialState: MenuState = {
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    };

    // 1. 設置選中菜單
    let state = menuReducer(initialState, setSelectedMenuId('11'));
    expect(state.selectedMenuId).toBe('11');

    // 2. 設置展開的子菜單
    state = menuReducer(state, setOpenKeys(['1']));
    expect(state.openKeys).toEqual(['1']);

    // 3. 切換到折疊狀態（openKeys 應該被清空）
    state = menuReducer(state, toggleCollapsed());
    expect(state.collapsed).toBe(true);
    expect(state.openKeys).toEqual([]);
    expect(state.selectedMenuId).toBe('11'); // selectedMenuId 不應該被清空

    // 4. 重置菜單狀態
    state = menuReducer(state, resetMenuState());
    expect(state).toEqual({
      collapsed: false,
      selectedMenuId: '',
      openKeys: [],
    });
  });
});
