/**
 * TagNavSlice Unit Tests
 * tagNavSlice 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect } from 'vitest';
import tagNavReducer, {
  addTag,
  removeTag,
  removeOtherTags,
  removeAllTags,
  setActiveTag,
  refreshTag,
  toggleKeepAlive,
  resetTagNav,
  selectTags,
  selectActiveTagPath,
  selectActiveTag,
  selectCachedPaths,
  selectKeepAliveEnabled,
} from './tagNavSlice';
import type { TagNavItem } from './tagNavSlice';

describe('tagNavSlice', () => {
  // 初始狀態
  const initialState = {
    tags: [
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

  // ==================== Reducer 測試 ====================

  describe('Reducer', () => {
    it('should return the initial state', () => {
      expect(tagNavReducer(undefined, { type: 'unknown' })).toEqual(initialState);
    });
  });

  // ==================== addTag ====================

  describe('addTag', () => {
    it('should add a new tag', () => {
      const newTag: TagNavItem = {
        path: '/system/employee',
        title: '員工管理',
      };

      const state = tagNavReducer(initialState, addTag(newTag));

      expect(state.tags).toHaveLength(2);
      expect(state.tags[1]).toEqual(newTag);
      expect(state.activeTagPath).toBe('/system/employee');
      expect(state.cachedPaths).toContain('/system/employee');
    });

    it('should update existing tag and activate it', () => {
      const existingTag: TagNavItem = {
        path: '/home',
        title: '首頁更新',
        fixed: true,
      };

      const state = tagNavReducer(initialState, addTag(existingTag));

      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].title).toBe('首頁更新');
      expect(state.activeTagPath).toBe('/home');
    });

    it('should not add to cache when Keep-Alive is disabled', () => {
      const stateWithoutKeepAlive = {
        ...initialState,
        keepAliveEnabled: false,
        cachedPaths: [],
      };

      const newTag: TagNavItem = {
        path: '/system/role',
        title: '角色管理',
      };

      const state = tagNavReducer(stateWithoutKeepAlive, addTag(newTag));

      expect(state.cachedPaths).not.toContain('/system/role');
    });
  });

  // ==================== removeTag ====================

  describe('removeTag', () => {
    it('should remove a tag', () => {
      const stateWithTwoTags = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
        ],
        activeTagPath: '/home',
        cachedPaths: ['/home', '/system/employee'],
      };

      const state = tagNavReducer(stateWithTwoTags, removeTag('/system/employee'));

      expect(state.tags).toHaveLength(1);
      expect(state.tags.find(t => t.path === '/system/employee')).toBeUndefined();
      expect(state.cachedPaths).not.toContain('/system/employee');
    });

    it('should not remove fixed tag', () => {
      const state = tagNavReducer(initialState, removeTag('/home'));

      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].path).toBe('/home');
    });

    it('should activate adjacent tag when removing active tag', () => {
      const stateWithThreeTags = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
          { path: '/system/role', title: '角色管理' },
        ],
        activeTagPath: '/system/employee',
        cachedPaths: ['/home', '/system/employee', '/system/role'],
      };

      const state = tagNavReducer(stateWithThreeTags, removeTag('/system/employee'));

      // 應該激活右側標籤
      expect(state.activeTagPath).toBe('/system/role');
    });

    it('should activate left tag when removing last tag', () => {
      const stateWithTwoTags = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
        ],
        activeTagPath: '/system/employee',
        cachedPaths: ['/home', '/system/employee'],
      };

      const state = tagNavReducer(stateWithTwoTags, removeTag('/system/employee'));

      // 應該激活左側標籤
      expect(state.activeTagPath).toBe('/home');
    });
  });

  // ==================== removeOtherTags ====================

  describe('removeOtherTags', () => {
    it('should remove all tags except current and fixed tags', () => {
      const stateWithMultipleTags = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
          { path: '/system/role', title: '角色管理' },
          { path: '/system/menu', title: '菜單管理' },
        ],
        activeTagPath: '/system/role',
        cachedPaths: ['/home', '/system/employee', '/system/role', '/system/menu'],
      };

      const state = tagNavReducer(stateWithMultipleTags, removeOtherTags('/system/role'));

      expect(state.tags).toHaveLength(2);
      expect(state.tags.find(t => t.path === '/home')).toBeDefined(); // 固定標籤保留
      expect(state.tags.find(t => t.path === '/system/role')).toBeDefined(); // 當前標籤保留
      expect(state.tags.find(t => t.path === '/system/employee')).toBeUndefined();
      expect(state.tags.find(t => t.path === '/system/menu')).toBeUndefined();
      expect(state.activeTagPath).toBe('/system/role');
    });
  });

  // ==================== removeAllTags ====================

  describe('removeAllTags', () => {
    it('should remove all tags except fixed tags', () => {
      const stateWithMultipleTags = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
          { path: '/system/role', title: '角色管理' },
        ],
        activeTagPath: '/system/role',
        cachedPaths: ['/home', '/system/employee', '/system/role'],
      };

      const state = tagNavReducer(stateWithMultipleTags, removeAllTags());

      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].path).toBe('/home');
      expect(state.activeTagPath).toBe('/home');
      expect(state.cachedPaths).toEqual(['/home']);
    });
  });

  // ==================== setActiveTag ====================

  describe('setActiveTag', () => {
    it('should set active tag', () => {
      const stateWithTwoTags = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
        ],
        activeTagPath: '/home',
        cachedPaths: ['/home', '/system/employee'],
      };

      const state = tagNavReducer(stateWithTwoTags, setActiveTag('/system/employee'));

      expect(state.activeTagPath).toBe('/system/employee');
    });

    it('should not change active tag if path not found', () => {
      const state = tagNavReducer(initialState, setActiveTag('/non-existent'));

      expect(state.activeTagPath).toBe('/home');
    });
  });

  // ==================== refreshTag ====================

  describe('refreshTag', () => {
    it('should remove tag from cache', () => {
      const stateWithCache = {
        ...initialState,
        cachedPaths: ['/home', '/system/employee'],
      };

      const state = tagNavReducer(stateWithCache, refreshTag('/system/employee'));

      expect(state.cachedPaths).not.toContain('/system/employee');
      expect(state.cachedPaths).toContain('/home');
    });
  });

  // ==================== toggleKeepAlive ====================

  describe('toggleKeepAlive', () => {
    it('should toggle Keep-Alive state', () => {
      const state = tagNavReducer(initialState, toggleKeepAlive());

      expect(state.keepAliveEnabled).toBe(false);
      expect(state.cachedPaths).toEqual([]);
    });

    it('should restore cache when re-enabling Keep-Alive', () => {
      const stateWithoutKeepAlive = {
        ...initialState,
        keepAliveEnabled: false,
        cachedPaths: [],
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
        ],
      };

      const state = tagNavReducer(stateWithoutKeepAlive, toggleKeepAlive());

      expect(state.keepAliveEnabled).toBe(true);
      expect(state.cachedPaths).toEqual(['/home', '/system/employee']);
    });
  });

  // ==================== resetTagNav ====================

  describe('resetTagNav', () => {
    it('should reset to initial state', () => {
      const modifiedState = {
        ...initialState,
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
        ],
        activeTagPath: '/system/employee',
        cachedPaths: ['/home', '/system/employee'],
      };

      const state = tagNavReducer(modifiedState, resetTagNav());

      expect(state).toEqual(initialState);
    });
  });

  // ==================== Selectors 測試 ====================

  describe('Selectors', () => {
    const mockRootState = {
      tagNav: {
        tags: [
          { path: '/home', title: '首頁', fixed: true },
          { path: '/system/employee', title: '員工管理' },
        ],
        activeTagPath: '/system/employee',
        keepAliveEnabled: true,
        cachedPaths: ['/home', '/system/employee'],
      },
    } as any;

    it('selectTags should return all tags', () => {
      expect(selectTags(mockRootState)).toHaveLength(2);
    });

    it('selectActiveTagPath should return active tag path', () => {
      expect(selectActiveTagPath(mockRootState)).toBe('/system/employee');
    });

    it('selectActiveTag should return active tag', () => {
      const activeTag = selectActiveTag(mockRootState);
      expect(activeTag?.path).toBe('/system/employee');
      expect(activeTag?.title).toBe('員工管理');
    });

    it('selectCachedPaths should return cached paths', () => {
      expect(selectCachedPaths(mockRootState)).toEqual(['/home', '/system/employee']);
    });

    it('selectKeepAliveEnabled should return Keep-Alive state', () => {
      expect(selectKeepAliveEnabled(mockRootState)).toBe(true);
    });
  });
});
