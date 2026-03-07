/**
 * tagNavSlice unit tests
 */
import { describe, it, expect } from 'vitest';
import tagNavReducer, {
  addTag,
  setActiveKey,
  removeTag,
  removeOtherTags,
  removeAllTags,
  clearTagNav,
  type TagNavState,
} from '@/store/slices/tagNavSlice';

const initialState: TagNavState = {
  tagList: [],
  activeKey: '',
};

describe('tagNavSlice', () => {
  describe('addTag', () => {
    it('should add a new tag', () => {
      const state = tagNavReducer(
        initialState,
        addTag({ tag: { path: '/system/employee', title: 'Employee' } })
      );

      expect(state.tagList).toHaveLength(1);
      expect(state.tagList[0].path).toBe('/system/employee');
      expect(state.tagList[0].title).toBe('Employee');
      expect(state.activeKey).toBe('/system/employee');
    });

    it('should not add duplicate tag', () => {
      const stateWithTag: TagNavState = {
        tagList: [{ path: '/system/employee', title: 'Employee' }],
        activeKey: '/system/employee',
      };

      const state = tagNavReducer(
        stateWithTag,
        addTag({ tag: { path: '/system/employee', title: 'Employee Updated' } })
      );

      expect(state.tagList).toHaveLength(1);
      expect(state.activeKey).toBe('/system/employee');
    });

    it('should update fromPath on existing tag', () => {
      const stateWithTag: TagNavState = {
        tagList: [{ path: '/system/employee', title: 'Employee' }],
        activeKey: '/system/employee',
      };

      const state = tagNavReducer(
        stateWithTag,
        addTag({
          tag: { path: '/system/employee', title: 'Employee' },
          fromPath: '/system/role',
        })
      );

      expect(state.tagList).toHaveLength(1);
      expect(state.tagList[0].fromPath).toBe('/system/role');
    });

    it('should not add home path', () => {
      const state = tagNavReducer(
        initialState,
        addTag({ tag: { path: '/home', title: 'Home' } })
      );

      expect(state.tagList).toHaveLength(0);
    });

    it('should enforce MAX_KEEP_ALIVE limit (30)', () => {
      let state: TagNavState = { ...initialState };

      // Add 30 tags
      for (let i = 0; i < 30; i++) {
        state = tagNavReducer(
          state,
          addTag({ tag: { path: `/page/${i}`, title: `Page ${i}` } })
        );
      }
      expect(state.tagList).toHaveLength(30);
      expect(state.tagList[0].path).toBe('/page/0');

      // Add 31st tag — should remove the first one
      state = tagNavReducer(
        state,
        addTag({ tag: { path: '/page/30', title: 'Page 30' } })
      );
      expect(state.tagList).toHaveLength(30);
      expect(state.tagList[0].path).toBe('/page/1');
      expect(state.tagList[29].path).toBe('/page/30');
    });

    it('should store icon and query', () => {
      const state = tagNavReducer(
        initialState,
        addTag({
          tag: {
            path: '/system/employee',
            title: 'Employee',
            icon: 'UserOutlined',
            query: { page: '1' },
          },
        })
      );

      expect(state.tagList[0].icon).toBe('UserOutlined');
      expect(state.tagList[0].query).toEqual({ page: '1' });
    });
  });

  describe('setActiveKey', () => {
    it('should set active key', () => {
      const state = tagNavReducer(initialState, setActiveKey('/system/role'));

      expect(state.activeKey).toBe('/system/role');
    });
  });

  describe('removeTag', () => {
    it('should remove a tag by path', () => {
      const stateWithTags: TagNavState = {
        tagList: [
          { path: '/system/employee', title: 'Employee' },
          { path: '/system/role', title: 'Role' },
        ],
        activeKey: '/system/employee',
      };

      const state = tagNavReducer(stateWithTags, removeTag('/system/employee'));

      expect(state.tagList).toHaveLength(1);
      expect(state.tagList[0].path).toBe('/system/role');
    });

    it('should do nothing for non-existent path', () => {
      const stateWithTags: TagNavState = {
        tagList: [{ path: '/system/employee', title: 'Employee' }],
        activeKey: '/system/employee',
      };

      const state = tagNavReducer(stateWithTags, removeTag('/non-existent'));

      expect(state.tagList).toHaveLength(1);
    });
  });

  describe('removeOtherTags', () => {
    it('should keep only the specified tag', () => {
      const stateWithTags: TagNavState = {
        tagList: [
          { path: '/a', title: 'A' },
          { path: '/b', title: 'B' },
          { path: '/c', title: 'C' },
        ],
        activeKey: '/b',
      };

      const state = tagNavReducer(stateWithTags, removeOtherTags('/b'));

      expect(state.tagList).toHaveLength(1);
      expect(state.tagList[0].path).toBe('/b');
    });
  });

  describe('removeAllTags', () => {
    it('should remove all tags and reset activeKey', () => {
      const stateWithTags: TagNavState = {
        tagList: [
          { path: '/a', title: 'A' },
          { path: '/b', title: 'B' },
        ],
        activeKey: '/a',
      };

      const state = tagNavReducer(stateWithTags, removeAllTags());

      expect(state.tagList).toHaveLength(0);
      expect(state.activeKey).toBe('');
    });
  });

  describe('clearTagNav', () => {
    it('should clear all state (used on logout)', () => {
      const stateWithTags: TagNavState = {
        tagList: [
          { path: '/a', title: 'A' },
          { path: '/b', title: 'B' },
        ],
        activeKey: '/a',
      };

      const state = tagNavReducer(stateWithTags, clearTagNav());

      expect(state.tagList).toHaveLength(0);
      expect(state.activeKey).toBe('');
    });
  });
});
