/**
 * Role Slice
 * 角色權限樹狀態管理
 *
 * 用於角色權限樹的選中狀態管理，包括：
 * - 初始化選中數據
 * - 添加/刪除選中項及其子項
 * - 自動選中上級節點
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { RoleState, MenuTreeNode } from '@/types/role';

const initialState: RoleState = {
  checkedData: [],
  treeMap: {},
};

const roleSlice = createSlice({
  name: 'role',
  initialState,
  reducers: {
    /**
     * 初始化選中數據（去重）
     */
    initCheckedData: (state, action: PayloadAction<number[]>) => {
      state.checkedData = [...new Set(action.payload)];
    },

    /**
     * 添加單個選中項
     */
    addCheckedData: (state, action: PayloadAction<number>) => {
      if (!state.checkedData.includes(action.payload)) {
        state.checkedData.push(action.payload);
      }
    },

    /**
     * 添加選中項及其所有子項
     * @param data - 菜單樹節點
     */
    addCheckedDataAndChildren: (state, action: PayloadAction<MenuTreeNode>) => {
      const addRecursive = (node: MenuTreeNode) => {
        if (node.menuId && !state.checkedData.includes(node.menuId)) {
          state.checkedData.push(node.menuId);
        }
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => addRecursive(child));
        }
      };
      addRecursive(action.payload);
    },

    /**
     * 刪除單個選中項（按索引）
     */
    deleteCheckedData: (state, action: PayloadAction<number>) => {
      const index = action.payload;
      if (index >= 0 && index < state.checkedData.length) {
        state.checkedData.splice(index, 1);
      }
    },

    /**
     * 刪除選中項（按 menuId）
     */
    deleteCheckedDataByMenuId: (state, action: PayloadAction<number>) => {
      state.checkedData = state.checkedData.filter(id => id !== action.payload);
    },

    /**
     * 刪除選中項及其所有子項
     * @param data - 菜單樹節點
     */
    deleteCheckedDataAndChildren: (state, action: PayloadAction<MenuTreeNode>) => {
      const deleteRecursive = (node: MenuTreeNode) => {
        if (node.menuId) {
          state.checkedData = state.checkedData.filter(id => id !== node.menuId);
        }
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => deleteRecursive(child));
        }
      };
      deleteRecursive(action.payload);
    },

    /**
     * 初始化權限樹 Map
     * @param tree - 菜單樹
     */
    initTreeMap: (state, action: PayloadAction<MenuTreeNode[]>) => {
      const buildMap = (nodes: MenuTreeNode[]) => {
        nodes.forEach(node => {
          if (node.menuId) {
            state.treeMap[node.menuId] = node;
          }
          if (node.children && node.children.length > 0) {
            buildMap(node.children);
          }
        });
      };
      state.treeMap = {};
      buildMap(action.payload);
    },

    /**
     * 選中上級節點（遞歸）
     * @param module - 當前菜單節點
     */
    selectUpperLevel: (state, action: PayloadAction<MenuTreeNode>) => {
      const selectParentRecursive = (node: MenuTreeNode) => {
        if (!node.parentId) {
          return;
        }
        const parentNode = state.treeMap[node.parentId];
        if (!parentNode) {
          return;
        }
        // 選中父節點
        if (parentNode.menuId && !state.checkedData.includes(parentNode.menuId)) {
          state.checkedData.push(parentNode.menuId);
        }
        // 遞歸選中上級
        if (parentNode.parentId) {
          selectParentRecursive(parentNode);
        }
      };
      selectParentRecursive(action.payload);
    },

    /**
     * 重置狀態
     */
    reset: () => initialState,
  },
});

export const {
  initCheckedData,
  addCheckedData,
  addCheckedDataAndChildren,
  deleteCheckedData,
  deleteCheckedDataByMenuId,
  deleteCheckedDataAndChildren,
  initTreeMap,
  selectUpperLevel,
  reset,
} = roleSlice.actions;

export default roleSlice.reducer;
