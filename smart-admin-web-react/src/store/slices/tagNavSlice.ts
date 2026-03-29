/**
 * TagNav Slice
 * 標籤導航狀態管理（頁籤導航）
 *
 * 參考：Vue 版本 smart-admin-web/src/store/modules/model/UserTagNav.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../index';

// ==================== State 類型定義 ====================

/**
 * 標籤導航項
 */
export interface TagNavItem {
  /** 菜單路徑（唯一標識） */
  path: string;

  /** 菜單標題 */
  title: string;

  /** 路由查詢參數（可選） */
  query?: Record<string, string>;

  /** 來源菜單路徑（用於返回導航） */
  fromPath?: string;

  /** 來源菜單查詢參數 */
  fromQuery?: Record<string, string>;

  /** 是否固定（首頁等不可關閉的標籤） */
  fixed?: boolean;
}

export interface TagNavState {
  /** 標籤列表 */
  tags: TagNavItem[];

  /** 當前活動標籤路徑 */
  activeTagPath: string;

  /** 是否啟用 Keep-Alive 緩存 */
  keepAliveEnabled: boolean;

  /** 緩存的路由路徑列表 */
  cachedPaths: string[];
}

// ==================== 初始狀態 ====================

const initialState: TagNavState = {
  tags: [
    // 默認添加首頁標籤（固定）
    {
      path: '/home',
      title: '首頁',
      fixed: true,
    },
  ],
  activeTagPath: '/home',
  keepAliveEnabled: true,
  cachedPaths: ['/home'],
};

// ==================== Slice 定義 ====================

const tagNavSlice = createSlice({
  name: 'tagNav',
  initialState,
  reducers: {
    /**
     * 添加標籤
     * 如果標籤已存在，則僅激活該標籤
     */
    addTag: (state, action: PayloadAction<TagNavItem>) => {
      const newTag = action.payload;
      const existingTagIndex = state.tags.findIndex(tag => tag.path === newTag.path);

      if (existingTagIndex > -1) {
        // 標籤已存在，更新標籤信息並激活
        state.tags[existingTagIndex] = {
          ...state.tags[existingTagIndex],
          ...newTag,
        };
      } else {
        // 新標籤，添加到列表
        state.tags.push(newTag);
      }

      // 激活新標籤
      state.activeTagPath = newTag.path;

      // 添加到緩存列表（如果啟用 Keep-Alive）
      if (state.keepAliveEnabled && !state.cachedPaths.includes(newTag.path)) {
        state.cachedPaths.push(newTag.path);
      }
    },

    /**
     * 移除標籤
     * 固定標籤不可移除
     */
    removeTag: (state, action: PayloadAction<string>) => {
      const pathToRemove = action.payload;
      const tagIndex = state.tags.findIndex(tag => tag.path === pathToRemove);

      if (tagIndex === -1) return;

      const tag = state.tags[tagIndex];

      // 固定標籤不可移除
      if (tag.fixed) return;

      // 如果刪除的是當前活動標籤，需要激活相鄰標籤
      if (state.activeTagPath === pathToRemove) {
        // 優先激活右側標籤，如果沒有則激活左側標籤
        const nextTag = state.tags[tagIndex + 1] || state.tags[tagIndex - 1];
        if (nextTag) {
          state.activeTagPath = nextTag.path;
        }
      }

      // 從標籤列表移除
      state.tags.splice(tagIndex, 1);

      // 從緩存列表移除
      const cachedIndex = state.cachedPaths.indexOf(pathToRemove);
      if (cachedIndex > -1) {
        state.cachedPaths.splice(cachedIndex, 1);
      }
    },

    /**
     * 移除其他標籤
     * 保留當前標籤和固定標籤
     */
    removeOtherTags: (state, action: PayloadAction<string>) => {
      const currentPath = action.payload;
      state.tags = state.tags.filter(tag => tag.fixed || tag.path === currentPath);
      state.cachedPaths = state.tags.map(tag => tag.path);
      state.activeTagPath = currentPath;
    },

    /**
     * 移除所有標籤
     * 僅保留固定標籤
     */
    removeAllTags: state => {
      state.tags = state.tags.filter(tag => tag.fixed);
      state.cachedPaths = state.tags.map(tag => tag.path);
      // 激活第一個固定標籤
      if (state.tags.length > 0) {
        state.activeTagPath = state.tags[0].path;
      }
    },

    /**
     * 設置活動標籤
     */
    setActiveTag: (state, action: PayloadAction<string>) => {
      const path = action.payload;
      const tag = state.tags.find(t => t.path === path);
      if (tag) {
        state.activeTagPath = path;
      }
    },

    /**
     * 刷新標籤
     * 從緩存中移除，下次訪問時重新加載
     */
    refreshTag: (state, action: PayloadAction<string>) => {
      const pathToRefresh = action.payload;
      const cachedIndex = state.cachedPaths.indexOf(pathToRefresh);
      if (cachedIndex > -1) {
        state.cachedPaths.splice(cachedIndex, 1);
      }
      // 下次添加標籤時會重新添加到緩存
    },

    /**
     * 切換 Keep-Alive 狀態
     */
    toggleKeepAlive: state => {
      state.keepAliveEnabled = !state.keepAliveEnabled;
      if (!state.keepAliveEnabled) {
        // 禁用時清空緩存列表
        state.cachedPaths = [];
      } else {
        // 啟用時重新緩存所有標籤
        state.cachedPaths = state.tags.map(tag => tag.path);
      }
    },

    /**
     * 重置標籤導航
     */
    resetTagNav: () => initialState,
  },
});

// ==================== Actions ====================

export const {
  addTag,
  removeTag,
  removeOtherTags,
  removeAllTags,
  setActiveTag,
  refreshTag,
  toggleKeepAlive,
  resetTagNav,
} = tagNavSlice.actions;

// ==================== Selectors ====================

/** 獲取所有標籤列表 */
export const selectTags = (state: RootState) => state.tagNav.tags;

/** 獲取當前活動標籤路徑 */
export const selectActiveTagPath = (state: RootState) => state.tagNav.activeTagPath;

/** 獲取當前活動標籤 */
export const selectActiveTag = (state: RootState) =>
  state.tagNav.tags.find(tag => tag.path === state.tagNav.activeTagPath);

/** 獲取緩存的路由路徑列表 */
export const selectCachedPaths = (state: RootState) => state.tagNav.cachedPaths;

/** 獲取 Keep-Alive 啟用狀態 */
export const selectKeepAliveEnabled = (state: RootState) => state.tagNav.keepAliveEnabled;

/** 判斷標籤是否可關閉 */
export const selectTagClosable = (path: string) => (state: RootState) => {
  const tag = state.tagNav.tags.find(t => t.path === path);
  return tag ? !tag.fixed : false;
};

/** Aliases for backward compatibility */
export const selectTagList = selectTags;
export const selectActiveKey = selectActiveTagPath;
export const { setActiveKey } = { setActiveKey: tagNavSlice.actions.setActiveTag };

// ==================== Reducer ====================

export default tagNavSlice.reducer;
