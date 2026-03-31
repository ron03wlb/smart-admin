/**
 * tagNavSlice unit tests
 */
import { describe, it, expect } from 'vitest';
import tagNavReducer, {
  addTag,
  setActiveTag,
  removeTag,
  removeOtherTags,
  removeAllTags,
  resetTagNav,
  type TagNavState,
} from '@/store/slices/tagNavSlice';

// The real initial state has a fixed '/home' tag
const defaultInitialState: TagNavState = {
  tags: [{ path: '/home', title: '首頁', fixed: true }],
  activeTagPath: '/home',
  keepAliveEnabled: true,
  cachedPaths: ['/home'],
};

describe('tagNavSlice', () => {
  describe('addTag', () => {
    it('should add a new tag', () => {
      const state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );

      expect(state.tags).toHaveLength(2);
      expect(state.tags[1].path).toBe('/system/employee');
      expect(state.tags[1].title).toBe('Employee');
      expect(state.activeTagPath).toBe('/system/employee');
    });

    it('should not add duplicate tag', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );

      state = tagNavReducer(
        state,
        addTag({ path: '/system/employee', title: 'Employee Updated' })
      );

      // Still 2 tags (home + employee), not 3
      expect(state.tags).toHaveLength(2);
      expect(state.activeTagPath).toBe('/system/employee');
    });

    it('should update existing tag info when re-added', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );

      state = tagNavReducer(
        state,
        addTag({ path: '/system/employee', title: 'Employee Updated', query: { id: '1' } })
      );

      expect(state.tags).toHaveLength(2);
      expect(state.tags[1].title).toBe('Employee Updated');
      expect(state.tags[1].query).toEqual({ id: '1' });
    });

    it('should store query params', () => {
      const state = tagNavReducer(
        defaultInitialState,
        addTag({
          path: '/system/employee',
          title: 'Employee',
          query: { page: '1' },
        })
      );

      expect(state.tags[1].query).toEqual({ page: '1' });
    });

    it('should add to cachedPaths when keepAlive is enabled', () => {
      const state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );

      expect(state.cachedPaths).toContain('/system/employee');
    });
  });

  describe('setActiveTag', () => {
    it('should set active tag path for existing tag', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );

      state = tagNavReducer(state, setActiveTag('/home'));
      expect(state.activeTagPath).toBe('/home');
    });
  });

  describe('removeTag', () => {
    it('should remove a non-fixed tag by path', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );

      state = tagNavReducer(state, removeTag('/system/employee'));
      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].path).toBe('/home');
    });

    it('should not remove fixed tags', () => {
      const state = tagNavReducer(defaultInitialState, removeTag('/home'));

      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].path).toBe('/home');
    });

    it('should do nothing for non-existent path', () => {
      const state = tagNavReducer(defaultInitialState, removeTag('/non-existent'));
      expect(state.tags).toHaveLength(1);
    });

    it('should activate adjacent tag when removing active tag', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/system/employee', title: 'Employee' })
      );
      state = tagNavReducer(state, addTag({ path: '/system/role', title: 'Role' }));

      // Active is now /system/role, remove it
      state = tagNavReducer(state, removeTag('/system/role'));
      // Should activate the previous tag
      expect(state.activeTagPath).toBe('/system/employee');
    });
  });

  describe('removeOtherTags', () => {
    it('should keep only the specified tag and fixed tags', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/a', title: 'A' })
      );
      state = tagNavReducer(state, addTag({ path: '/b', title: 'B' }));
      state = tagNavReducer(state, addTag({ path: '/c', title: 'C' }));

      state = tagNavReducer(state, removeOtherTags('/b'));

      // Should keep /home (fixed) and /b
      expect(state.tags).toHaveLength(2);
      expect(state.tags.map(t => t.path)).toContain('/home');
      expect(state.tags.map(t => t.path)).toContain('/b');
    });
  });

  describe('removeAllTags', () => {
    it('should remove all non-fixed tags', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/a', title: 'A' })
      );
      state = tagNavReducer(state, addTag({ path: '/b', title: 'B' }));

      state = tagNavReducer(state, removeAllTags());

      // Only fixed /home remains
      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].path).toBe('/home');
      expect(state.activeTagPath).toBe('/home');
    });
  });

  describe('resetTagNav', () => {
    it('should reset to initial state (used on logout)', () => {
      let state = tagNavReducer(
        defaultInitialState,
        addTag({ path: '/a', title: 'A' })
      );
      state = tagNavReducer(state, addTag({ path: '/b', title: 'B' }));

      state = tagNavReducer(state, resetTagNav());

      expect(state.tags).toHaveLength(1);
      expect(state.tags[0].path).toBe('/home');
      expect(state.activeTagPath).toBe('/home');
    });
  });
});
