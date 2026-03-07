/**
 * routeBuilder 單元測試
 */
import { describe, it, expect } from 'vitest';
import { buildMenuTree, buildMenuParentMap } from '@/utils/routeBuilder';
import type { MenuItem } from '@/types/user.types';

describe('buildMenuTree', () => {
  it('should build tree from flat menu list', () => {
    const menuList: MenuItem[] = [
      { menuId: '1', menuName: '系統管理', menuType: 'CATALOG', parentId: '0', visibleFlag: true, disabledFlag: false, sort: 1 },
      { menuId: '11', menuName: '員工管理', menuType: 'MENU', parentId: '1', path: '/system/employee', visibleFlag: true, disabledFlag: false, sort: 1 },
      { menuId: '12', menuName: '角色管理', menuType: 'MENU', parentId: '1', path: '/system/role', visibleFlag: true, disabledFlag: false, sort: 2 },
      { menuId: '2', menuName: '業務中心', menuType: 'CATALOG', parentId: '0', visibleFlag: true, disabledFlag: false, sort: 2 },
      { menuId: '21', menuName: '商品管理', menuType: 'MENU', parentId: '2', path: '/business/goods', visibleFlag: true, disabledFlag: false, sort: 1 },
    ];

    const tree = buildMenuTree(menuList);

    expect(tree).toHaveLength(2);
    expect(tree[0].menuName).toBe('系統管理');
    expect(tree[0].children).toHaveLength(2);
    expect(tree[0].children![0].menuName).toBe('員工管理');
    expect(tree[0].children![1].menuName).toBe('角色管理');
    expect(tree[1].menuName).toBe('業務中心');
    expect(tree[1].children).toHaveLength(1);
  });

  it('should filter out POINTS type', () => {
    const menuList: MenuItem[] = [
      { menuId: '1', menuName: '系統管理', menuType: 'CATALOG', parentId: '0', visibleFlag: true, disabledFlag: false },
      { menuId: '11', menuName: '新增員工', menuType: 'POINTS', parentId: '1', visibleFlag: true, disabledFlag: false, webPerms: 'system:employee:add' },
    ];

    const tree = buildMenuTree(menuList);

    expect(tree).toHaveLength(1);
    expect(tree[0].children).toBeUndefined();
  });

  it('should filter out invisible menus', () => {
    const menuList: MenuItem[] = [
      { menuId: '1', menuName: '系統管理', menuType: 'CATALOG', parentId: '0', visibleFlag: true, disabledFlag: false },
      { menuId: '11', menuName: '隱藏菜單', menuType: 'MENU', parentId: '1', visibleFlag: false, disabledFlag: false },
    ];

    const tree = buildMenuTree(menuList);

    expect(tree).toHaveLength(1);
    expect(tree[0].children).toBeUndefined();
  });

  it('should filter out disabled menus', () => {
    const menuList: MenuItem[] = [
      { menuId: '1', menuName: '系統管理', menuType: 'CATALOG', parentId: '0', visibleFlag: true, disabledFlag: false },
      { menuId: '11', menuName: '禁用菜單', menuType: 'MENU', parentId: '1', visibleFlag: true, disabledFlag: true },
    ];

    const tree = buildMenuTree(menuList);

    expect(tree).toHaveLength(1);
    expect(tree[0].children).toBeUndefined();
  });

  it('should sort by sort field', () => {
    const menuList: MenuItem[] = [
      { menuId: '2', menuName: '後排', menuType: 'CATALOG', parentId: '0', sort: 10, visibleFlag: true, disabledFlag: false },
      { menuId: '1', menuName: '前排', menuType: 'CATALOG', parentId: '0', sort: 1, visibleFlag: true, disabledFlag: false },
    ];

    const tree = buildMenuTree(menuList);

    expect(tree[0].menuName).toBe('前排');
    expect(tree[1].menuName).toBe('後排');
  });

  it('should handle 3-level nesting', () => {
    const menuList: MenuItem[] = [
      { menuId: '1', menuName: 'Level 1', menuType: 'CATALOG', parentId: '0', visibleFlag: true, disabledFlag: false },
      { menuId: '11', menuName: 'Level 2', menuType: 'CATALOG', parentId: '1', visibleFlag: true, disabledFlag: false },
      { menuId: '111', menuName: 'Level 3', menuType: 'MENU', parentId: '11', path: '/deep', visibleFlag: true, disabledFlag: false },
    ];

    const tree = buildMenuTree(menuList);

    expect(tree).toHaveLength(1);
    expect(tree[0].children).toHaveLength(1);
    expect(tree[0].children![0].children).toHaveLength(1);
    expect(tree[0].children![0].children![0].menuName).toBe('Level 3');
  });

  it('should return empty array for empty input', () => {
    expect(buildMenuTree([])).toEqual([]);
  });
});

describe('buildMenuParentMap', () => {
  it('should build parent chain map', () => {
    const tree: MenuItem[] = [
      {
        menuId: '1', menuName: '系統管理', menuType: 'CATALOG', visibleFlag: true, disabledFlag: false,
        children: [
          { menuId: '11', menuName: '員工管理', menuType: 'MENU', visibleFlag: true, disabledFlag: false },
          { menuId: '12', menuName: '角色管理', menuType: 'MENU', visibleFlag: true, disabledFlag: false },
        ],
      },
    ];

    const map = buildMenuParentMap(tree);

    expect(map['1']).toEqual([]);
    expect(map['11']).toEqual([{ id: '1', title: '系統管理' }]);
    expect(map['12']).toEqual([{ id: '1', title: '系統管理' }]);
  });

  it('should handle deep nesting', () => {
    const tree: MenuItem[] = [
      {
        menuId: '1', menuName: 'A', menuType: 'CATALOG', visibleFlag: true, disabledFlag: false,
        children: [
          {
            menuId: '11', menuName: 'B', menuType: 'CATALOG', visibleFlag: true, disabledFlag: false,
            children: [
              { menuId: '111', menuName: 'C', menuType: 'MENU', visibleFlag: true, disabledFlag: false },
            ],
          },
        ],
      },
    ];

    const map = buildMenuParentMap(tree);

    expect(map['111']).toEqual([
      { id: '1', title: 'A' },
      { id: '11', title: 'B' },
    ]);
  });
});
