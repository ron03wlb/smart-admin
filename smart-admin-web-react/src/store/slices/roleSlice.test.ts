/**
 * roleSlice Tests
 * roleSlice 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, beforeEach } from 'vitest';
import roleReducer, {
  initCheckedData,
  addCheckedData,
  addCheckedDataAndChildren,
  deleteCheckedData,
  deleteCheckedDataByMenuId,
  deleteCheckedDataAndChildren,
  initTreeMap,
  selectUpperLevel,
  reset,
} from './roleSlice';
import type { RoleState, MenuTreeNode } from '@/types/role';

// Mock 菜單樹數據
const mockMenuTree: MenuTreeNode[] = [
  {
    menuId: 1,
    parentId: null,
    menuName: '系統管理',
    menuType: 1,
    children: [
      {
        menuId: 11,
        parentId: 1,
        menuName: '用戶管理',
        menuType: 2,
        children: [
          {
            menuId: 111,
            parentId: 11,
            menuName: '新增用戶',
            menuType: 3,
          },
          {
            menuId: 112,
            parentId: 11,
            menuName: '編輯用戶',
            menuType: 3,
          },
        ],
      },
      {
        menuId: 12,
        parentId: 1,
        menuName: '角色管理',
        menuType: 2,
        children: [
          {
            menuId: 121,
            parentId: 12,
            menuName: '新增角色',
            menuType: 3,
          },
        ],
      },
    ],
  },
  {
    menuId: 2,
    parentId: null,
    menuName: '業務管理',
    menuType: 1,
    children: [
      {
        menuId: 21,
        parentId: 2,
        menuName: '商品管理',
        menuType: 2,
      },
    ],
  },
];

describe('roleSlice', () => {
  let initialState: RoleState;

  beforeEach(() => {
    initialState = {
      checkedData: [],
      treeMap: {},
    };
  });

  describe('初始狀態', () => {
    it('應該返回默認狀態', () => {
      const state = roleReducer(undefined, { type: '' });
      expect(state.checkedData).toEqual([]);
      expect(state.treeMap).toEqual({});
    });
  });

  describe('initCheckedData', () => {
    it('應該初始化選中數據', () => {
      const state = roleReducer(initialState, initCheckedData([1, 2, 3]));
      expect(state.checkedData).toEqual([1, 2, 3]);
    });

    it('應該去重選中數據', () => {
      const state = roleReducer(initialState, initCheckedData([1, 2, 2, 3, 1]));
      expect(state.checkedData).toEqual([1, 2, 3]);
    });

    it('應該處理空數組', () => {
      const state = roleReducer(initialState, initCheckedData([]));
      expect(state.checkedData).toEqual([]);
    });
  });

  describe('addCheckedData', () => {
    it('應該添加單個選中項', () => {
      const state = roleReducer(initialState, addCheckedData(5));
      expect(state.checkedData).toEqual([5]);
    });

    it('應該不重複添加已存在的項', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3]));
      state = roleReducer(state, addCheckedData(2));
      expect(state.checkedData).toEqual([1, 2, 3]);
    });

    it('應該添加多個不同的項', () => {
      let state = initialState;
      state = roleReducer(state, addCheckedData(1));
      state = roleReducer(state, addCheckedData(2));
      state = roleReducer(state, addCheckedData(3));
      expect(state.checkedData).toEqual([1, 2, 3]);
    });
  });

  describe('addCheckedDataAndChildren', () => {
    it('應該添加節點及其所有子節點', () => {
      const node: MenuTreeNode = {
        menuId: 1,
        parentId: null,
        menuName: '系統管理',
        menuType: 1,
        children: [
          {
            menuId: 11,
            parentId: 1,
            menuName: '用戶管理',
            menuType: 2,
            children: [
              {
                menuId: 111,
                parentId: 11,
                menuName: '新增用戶',
                menuType: 3,
              },
            ],
          },
        ],
      };
      const state = roleReducer(initialState, addCheckedDataAndChildren(node));
      expect(state.checkedData).toContain(1);
      expect(state.checkedData).toContain(11);
      expect(state.checkedData).toContain(111);
    });

    it('應該處理沒有子節點的節點', () => {
      const node: MenuTreeNode = {
        menuId: 5,
        parentId: 1,
        menuName: '單一菜單',
        menuType: 2,
      };
      const state = roleReducer(initialState, addCheckedDataAndChildren(node));
      expect(state.checkedData).toEqual([5]);
    });
  });

  describe('deleteCheckedData', () => {
    it('應該刪除指定索引的選中項', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3, 4, 5]));
      state = roleReducer(state, deleteCheckedData(2)); // 刪除索引 2 (值為 3)
      expect(state.checkedData).toEqual([1, 2, 4, 5]);
    });

    it('應該處理無效索引', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3]));
      state = roleReducer(state, deleteCheckedData(10)); // 無效索引
      expect(state.checkedData).toEqual([1, 2, 3]);
    });
  });

  describe('deleteCheckedDataByMenuId', () => {
    it('應該刪除指定 menuId 的選中項', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3, 4, 5]));
      state = roleReducer(state, deleteCheckedDataByMenuId(3));
      expect(state.checkedData).toEqual([1, 2, 4, 5]);
    });

    it('應該處理不存在的 menuId', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3]));
      state = roleReducer(state, deleteCheckedDataByMenuId(10));
      expect(state.checkedData).toEqual([1, 2, 3]);
    });
  });

  describe('deleteCheckedDataAndChildren', () => {
    it('應該刪除節點及其所有子節點', () => {
      let state = roleReducer(initialState, initCheckedData([1, 11, 111, 12, 121]));
      const node: MenuTreeNode = {
        menuId: 11,
        parentId: 1,
        menuName: '用戶管理',
        menuType: 2,
        children: [
          {
            menuId: 111,
            parentId: 11,
            menuName: '新增用戶',
            menuType: 3,
          },
        ],
      };
      state = roleReducer(state, deleteCheckedDataAndChildren(node));
      expect(state.checkedData).toEqual([1, 12, 121]);
      expect(state.checkedData).not.toContain(11);
      expect(state.checkedData).not.toContain(111);
    });

    it('應該處理沒有子節點的節點', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3]));
      const node: MenuTreeNode = {
        menuId: 2,
        parentId: null,
        menuName: '單一菜單',
        menuType: 2,
      };
      state = roleReducer(state, deleteCheckedDataAndChildren(node));
      expect(state.checkedData).toEqual([1, 3]);
    });
  });

  describe('initTreeMap', () => {
    it('應該初始化菜單樹 Map', () => {
      const state = roleReducer(initialState, initTreeMap(mockMenuTree));
      expect(state.treeMap[1]).toBeDefined();
      expect(state.treeMap[1].menuName).toBe('系統管理');
      expect(state.treeMap[11]).toBeDefined();
      expect(state.treeMap[11].menuName).toBe('用戶管理');
      expect(state.treeMap[111]).toBeDefined();
      expect(state.treeMap[111].menuName).toBe('新增用戶');
      expect(state.treeMap[2]).toBeDefined();
      expect(state.treeMap[21]).toBeDefined();
    });

    it('應該處理空樹', () => {
      const state = roleReducer(initialState, initTreeMap([]));
      expect(state.treeMap).toEqual({});
    });

    it('應該處理多次初始化（覆蓋）', () => {
      let state = roleReducer(initialState, initTreeMap(mockMenuTree));
      expect(Object.keys(state.treeMap).length).toBeGreaterThan(0);

      const newTree: MenuTreeNode[] = [
        {
          menuId: 100,
          parentId: null,
          menuName: '新樹',
          menuType: 1,
        },
      ];
      state = roleReducer(state, initTreeMap(newTree));
      expect(state.treeMap[100]).toBeDefined();
      expect(state.treeMap[1]).toBeUndefined(); // 舊樹被清除
    });
  });

  describe('selectUpperLevel', () => {
    beforeEach(() => {
      // 初始化樹 Map
      initialState = roleReducer(initialState, initTreeMap(mockMenuTree));
    });

    it('應該選中上級節點', () => {
      const state = roleReducer(
        initialState,
        selectUpperLevel(initialState.treeMap[111]) // 選中 111 的上級
      );
      expect(state.checkedData).toContain(11); // 上級 11 被選中
    });

    it('應該遞歸選中所有上級節點', () => {
      const state = roleReducer(
        initialState,
        selectUpperLevel(initialState.treeMap[111]) // 選中 111 的所有上級
      );
      expect(state.checkedData).toContain(11); // 直接上級
      expect(state.checkedData).toContain(1); // 祖先
    });

    it('應該處理已選中的上級（不重複）', () => {
      let state = roleReducer(initialState, initCheckedData([1, 11]));
      state = roleReducer(state, selectUpperLevel(initialState.treeMap[111]));
      expect(state.checkedData.filter(id => id === 11).length).toBe(1); // 11 不重複
      expect(state.checkedData.filter(id => id === 1).length).toBe(1); // 1 不重複
    });

    it('應該處理頂級節點（無上級）', () => {
      const state = roleReducer(
        initialState,
        selectUpperLevel(initialState.treeMap[1]) // 頂級節點
      );
      expect(state.checkedData).toEqual([]); // 無上級，不添加
    });
  });

  describe('reset', () => {
    it('應該重置狀態', () => {
      let state = roleReducer(initialState, initCheckedData([1, 2, 3]));
      state = roleReducer(state, initTreeMap(mockMenuTree));
      expect(state.checkedData.length).toBeGreaterThan(0);
      expect(Object.keys(state.treeMap).length).toBeGreaterThan(0);

      state = roleReducer(state, reset());
      expect(state.checkedData).toEqual([]);
      expect(state.treeMap).toEqual({});
    });
  });

  describe('完整工作流', () => {
    it('應該支持完整的角色權限樹選中流程', () => {
      // 1. 初始化樹 Map
      let state = roleReducer(initialState, initTreeMap(mockMenuTree));
      expect(Object.keys(state.treeMap).length).toBeGreaterThan(0);

      // 2. 初始化選中數據
      state = roleReducer(state, initCheckedData([1, 2]));
      expect(state.checkedData).toEqual([1, 2]);

      // 3. 添加單個節點
      state = roleReducer(state, addCheckedData(11));
      expect(state.checkedData).toContain(11);

      // 4. 添加節點及子節點
      const node = state.treeMap[12];
      state = roleReducer(state, addCheckedDataAndChildren(node));
      expect(state.checkedData).toContain(12);
      expect(state.checkedData).toContain(121);

      // 5. 選中上級
      state = roleReducer(state, selectUpperLevel(state.treeMap[21]));
      expect(state.checkedData).toContain(2); // 21 的上級

      // 6. 刪除節點及子節點
      state = roleReducer(state, deleteCheckedDataAndChildren(state.treeMap[12]));
      expect(state.checkedData).not.toContain(12);
      expect(state.checkedData).not.toContain(121);

      // 7. 重置
      state = roleReducer(state, reset());
      expect(state.checkedData).toEqual([]);
      expect(state.treeMap).toEqual({});
    });
  });

  describe('狀態不可變性', () => {
    it('應該不修改原始狀態', () => {
      const originalState = { ...initialState, checkedData: [1, 2, 3] };
      const state = roleReducer(originalState, addCheckedData(4));
      expect(originalState.checkedData).toEqual([1, 2, 3]);
      expect(state.checkedData).toContain(4);
    });
  });
});
