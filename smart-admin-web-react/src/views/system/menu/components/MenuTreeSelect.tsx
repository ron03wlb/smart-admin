/**
 * Menu Tree Select Component
 * 菜單樹選擇器組件
 *
 * 用於選擇上級菜單/目錄，支持：
 * - 自動構建樹形結構
 * - 禁用不符合規則的選項
 * - 層級限制（最多 3 層）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { useEffect, useState, useCallback } from 'react';
import { TreeSelect } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { menuApi } from '@/api/system/menuApi';
import type { MenuVO, MenuTypeEnum } from '../types';
import { MENU_CONSTANTS } from '@/constants/system/menuConst';

interface MenuTreeSelectProps {
  /** 當前值（菜單ID） */
  value?: number;

  /** 值變化回調 */
  onChange?: (value: number | undefined) => void;

  /** 當前菜單類型 */
  menuType?: MenuTypeEnum;

  /** 當前菜單ID（編輯時用於排除自身及子節點） */
  currentMenuId?: number;

  /** 是否禁用 */
  disabled?: boolean;

  /** 佔位符 */
  placeholder?: string;
}

/**
 * 樹節點類型
 */
interface TreeNode extends DataNode {
  value: number;
  title: string;
  key: number;
  children?: TreeNode[];
}

/**
 * 菜單樹選擇器
 */
export default function MenuTreeSelect({
  value,
  onChange,
  menuType,
  currentMenuId,
  disabled,
  placeholder = '請選擇上級菜單',
}: MenuTreeSelectProps) {
  const [treeData, setTreeData] = useState<TreeNode[]>([]);
  const [loading, setLoading] = useState(false);

  /**
   * 加載菜單列表並構建樹形數據
   */
  const loadMenuTree = useCallback(async () => {
    try {
      setLoading(true);
      const response = await menuApi.queryMenu();
      const menuList = response.data || [];

      // 構建樹形數據
      const tree = buildTreeData(menuList);

      // 添加頂級選項
      const treeDataWithTop: TreeNode[] = [
        {
          title: '頂級菜單/目錄',
          value: MENU_CONSTANTS.TOP_PARENT_ID,
          key: MENU_CONSTANTS.TOP_PARENT_ID,
        },
        ...tree,
      ];

      setTreeData(treeDataWithTop);
    } catch (error) {
      console.error('加載菜單樹失敗:', error);
    } finally {
      setLoading(false);
    }
  }, [menuType, currentMenuId]);

  /**
   * 監聽 menuType 和 currentMenuId 變化
   */
  useEffect(() => {
    loadMenuTree();
  }, [loadMenuTree]);

  /**
   * 構建樹形數據
   */
  const buildTreeData = (menuList: MenuVO[]): TreeNode[] => {
    // 過濾掉當前菜單及其子節點（編輯時）
    const filteredMenuList = filterCurrentAndChildren(menuList, currentMenuId);

    // 只保留可選的菜單類型
    const validMenuList = filterValidMenuTypes(filteredMenuList, menuType);

    // 構建樹形結構
    return buildTree(validMenuList, MENU_CONSTANTS.TOP_PARENT_ID);
  };

  /**
   * 過濾當前菜單及其所有子節點（避免循環引用）
   */
  const filterCurrentAndChildren = (menuList: MenuVO[], currentId?: number): MenuVO[] => {
    if (!currentId) return menuList;

    // 獲取所有子節點ID
    const childrenIds = getAllChildrenIds(menuList, currentId);

    // 排除當前節點和所有子節點
    return menuList.filter(menu => menu.menuId !== currentId && !childrenIds.includes(menu.menuId));
  };

  /**
   * 獲取所有子節點ID（遞歸）
   */
  const getAllChildrenIds = (menuList: MenuVO[], parentId: number): number[] => {
    const childrenIds: number[] = [];
    const children = menuList.filter(menu => menu.parentId === parentId);

    children.forEach(child => {
      childrenIds.push(child.menuId);
      // 遞歸獲取子節點的子節點
      const grandChildrenIds = getAllChildrenIds(menuList, child.menuId);
      childrenIds.push(...grandChildrenIds);
    });

    return childrenIds;
  };

  /**
   * 根據當前菜單類型過濾可選的父級菜單類型
   */
  const filterValidMenuTypes = (menuList: MenuVO[], currentType?: MenuTypeEnum): MenuVO[] => {
    if (!currentType) return menuList;

    return menuList.filter(menu => {
      // 根據 SmartAdmin 規則：
      // - 目錄的父級：只能是目錄或頂級（menuType=1）
      // - 菜單的父級：只能是目錄（menuType=1）
      // - 功能點的父級：只能是菜單（menuType=2）

      if (currentType === 1) {
        // 目錄只能選擇目錄作為父級
        return menu.menuType === 1;
      } else if (currentType === 2) {
        // 菜單只能選擇目錄作為父級
        return menu.menuType === 1;
      } else if (currentType === 3) {
        // 功能點只能選擇菜單作為父級
        return menu.menuType === 2;
      }

      return true;
    });
  };

  /**
   * 構建樹形結構（遞歸）
   */
  const buildTree = (menuList: MenuVO[], parentId: number): TreeNode[] => {
    const children = menuList.filter(menu => menu.parentId === parentId);

    if (children.length === 0) {
      return [];
    }

    return children
      .sort((a, b) => a.sort - b.sort)
      .map(menu => {
        const subChildren = buildTree(menuList, menu.menuId);

        const node: TreeNode = {
          title: menu.menuName,
          value: menu.menuId,
          key: menu.menuId,
        };

        if (subChildren.length > 0) {
          node.children = subChildren;
        }

        return node;
      });
  };

  /**
   * 處理值變化
   */
  const handleChange = (newValue: number) => {
    onChange?.(newValue === MENU_CONSTANTS.TOP_PARENT_ID ? undefined : newValue);
  };

  return (
    <TreeSelect<number>
      value={value || MENU_CONSTANTS.TOP_PARENT_ID}
      onChange={handleChange}
      treeData={treeData}
      loading={loading}
      disabled={disabled}
      placeholder={placeholder}
      showSearch
      treeDefaultExpandAll
      style={{ width: '100%' }}
      filterTreeNode={(input, node) => {
        const title = node.title as string;
        return title?.toLowerCase().includes(input.toLowerCase()) || false;
      }}
    />
  );
}
