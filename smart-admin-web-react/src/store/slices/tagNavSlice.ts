/**
 * TagNav 多標籤頁狀態管理
 *
 * 對應 Vue 的 tagNav 功能：
 * - 路由導航時自動添加標籤
 * - 關閉標籤時智能跳轉
 * - 數據持久化到 localStorage（透過 redux-persist）
 * - Keep-Alive 上限 30 個
 */
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

const MAX_KEEP_ALIVE = 30;

/**
 * 標籤項
 */
export interface TagItem {
  /** 路由路徑（唯一標識） */
  path: string;
  /** 顯示標題 */
  title: string;
  /** 菜單圖標名稱 */
  icon?: string;
  /** 路由 query 參數 */
  query?: Record<string, string>;
  /** 來源頁面路徑（關閉後跳轉用） */
  fromPath?: string;
}

/**
 * TagNav 狀態
 */
export interface TagNavState {
  /** 標籤列表 */
  tagList: TagItem[];
  /** 當前活動標籤路徑 */
  activeKey: string;
}

const initialState: TagNavState = {
  tagList: [],
  activeKey: '',
};

export const tagNavSlice = createSlice({
  name: 'tagNav',
  initialState,
  reducers: {
    /**
     * 添加或更新標籤（路由導航時調用）
     */
    addTag: (state, action: PayloadAction<{ tag: TagItem; fromPath?: string }>) => {
      const { tag, fromPath } = action.payload;

      // 排除首頁（首頁不需要標籤）
      if (tag.path === '/home' || tag.path === 'home') return;

      const existing = state.tagList.find((t) => t.path === tag.path);
      if (existing) {
        // 已存在 → 更新來源和 query
        existing.fromPath = fromPath;
        existing.query = tag.query;
      } else {
        // 不存在 → 添加新標籤
        if (state.tagList.length >= MAX_KEEP_ALIVE) {
          // 超出上限 → 移除最早的標籤
          state.tagList.shift();
        }
        state.tagList.push({ ...tag, fromPath });
      }
      state.activeKey = tag.path;
    },

    /**
     * 設置活動標籤
     */
    setActiveKey: (state, action: PayloadAction<string>) => {
      state.activeKey = action.payload;
    },

    /**
     * 關閉單個標籤
     */
    removeTag: (state, action: PayloadAction<string>) => {
      const path = action.payload;
      const index = state.tagList.findIndex((t) => t.path === path);
      if (index === -1) return;

      state.tagList.splice(index, 1);
    },

    /**
     * 關閉其他標籤（保留指定的）
     */
    removeOtherTags: (state, action: PayloadAction<string>) => {
      const keepPath = action.payload;
      state.tagList = state.tagList.filter((t) => t.path === keepPath);
    },

    /**
     * 關閉所有標籤
     */
    removeAllTags: (state) => {
      state.tagList = [];
      state.activeKey = '';
    },

    /**
     * 登出時清空
     */
    clearTagNav: (state) => {
      state.tagList = [];
      state.activeKey = '';
    },
  },
});

// ================================= Selectors =================================

export const selectTagList = (state: { tagNav: TagNavState }) => state.tagNav.tagList;
export const selectActiveKey = (state: { tagNav: TagNavState }) => state.tagNav.activeKey;

// ================================= Actions =================================

export const {
  addTag,
  setActiveKey,
  removeTag,
  removeOtherTags,
  removeAllTags,
  clearTagNav,
} = tagNavSlice.actions;

// ================================= Reducer =================================

export default tagNavSlice.reducer;
