/**
 * Menu Tree Select Component
 *
 * Corresponds to Vue's components/system/menu-tree-select/index.vue
 * Filters to show only CATALOG and MENU types, disabling CATALOG nodes.
 */
import React, { useEffect, useState, useCallback, useRef, forwardRef, useImperativeHandle } from 'react';
import { TreeSelect } from 'antd';
import { menuApi } from '@/api/system/menu-api';
import { MENU_TYPE_ENUM } from '@/constants/system/menu-const';
import type { MenuItem } from '@/types/user.types';

interface MenuTreeSelectProps {
  value?: string[];
  onChange?: (value: string[]) => void;
  placeholder?: string;
  style?: React.CSSProperties;
}

export interface MenuTreeSelectRef {
  getMenuListByIdList: (menuIdList: (string | number)[]) => MenuItem[];
}

/** Build tree from flat menu list */
function buildMenuTree(menuList: (MenuItem & { disabled?: boolean })[]): any[] {
  const map = new Map<string | number, any>();
  const roots: any[] = [];

  for (const item of menuList) {
    map.set(item.menuId, { ...item, children: [] });
  }

  for (const item of menuList) {
    const node = map.get(item.menuId)!;
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId)!.children.push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
}

const MenuTreeSelect = forwardRef<MenuTreeSelectRef, MenuTreeSelectProps>(({
  value,
  onChange,
  placeholder = '请选择菜单',
  style,
}, ref) => {
  const [treeData, setTreeData] = useState<any[]>([]);
  const menuListRef = useRef<(MenuItem & { disabled?: boolean })[]>([]);

  const queryMenuTree = useCallback(async () => {
    const res = await menuApi.query();
    if (res.code === 1 && res.data) {
      const filtered = res.data
        .filter((e) => e.menuType === String(MENU_TYPE_ENUM.MENU.value) || e.menuType === String(MENU_TYPE_ENUM.CATALOG.value))
        .map((item) => ({
          ...item,
          disabled: item.menuType === String(MENU_TYPE_ENUM.CATALOG.value),
        }));
      menuListRef.current = filtered;
      setTreeData(buildMenuTree(filtered));
    }
  }, []);

  useImperativeHandle(ref, () => ({
    getMenuListByIdList: (menuIdList: (string | number)[]) => {
      return menuListRef.current.filter((e) => menuIdList.includes(e.menuId));
    },
  }));

  useEffect(() => {
    queryMenuTree();
  }, [queryMenuTree]);

  return (
    <TreeSelect
      value={value}
      onChange={onChange}
      treeData={treeData}
      fieldNames={{ label: 'menuName', value: 'menuId', children: 'children' }}
      showSearch
      treeCheckable
      allowClear
      treeDefaultExpandAll
      placeholder={placeholder}
      style={{ width: '100%', ...style }}
      popupMatchSelectWidth={false}
      styles={{ popup: { root: { maxHeight: 400, overflow: 'auto' } } }}
      treeNodeFilterProp="menuName"
    />
  );
});

MenuTreeSelect.displayName = 'MenuTreeSelect';
export default MenuTreeSelect;
