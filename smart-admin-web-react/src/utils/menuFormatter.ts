/**
 * Menu Formatter
 * 菜單格式轉換器（MenuItem → Ant Design Menu ItemType）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import type { MenuProps } from 'antd';
import type { MenuItem } from '@/types/menu';
import * as Icons from '@ant-design/icons';

/**
 * 獲取 Ant Design 圖標組件
 *
 * @param iconName 圖標名稱（例如：'HomeOutlined', 'UserOutlined'）
 * @returns React Icon 組件
 */
function getAntdIcon(iconName?: string): React.ReactNode {
  if (!iconName) {
    return null;
  }

  // 動態獲取圖標組件
  const IconComponent = (Icons as any)[iconName];

  if (!IconComponent) {
    console.warn(`Icon "${iconName}" not found in @ant-design/icons`);
    return null;
  }

  return IconComponent({});
}

/**
 * 將 MenuItem 轉換為 Ant Design Menu ItemType
 *
 * @param menu 菜單項
 * @returns Ant Design Menu ItemType
 */
function convertMenuItemToAntdItem(menu: MenuItem): any {
  const item: any = {
    key: menu.path || menu.menuId.toString(),
    label: menu.menuName,
    icon: getAntdIcon(menu.icon),
  };

  // 遞歸轉換子菜單
  if (menu.children && menu.children.length > 0) {
    item.children = menu.children.map(convertMenuItemToAntdItem);
  }

  return item;
}

/**
 * 批量轉換菜單樹為 Ant Design Menu Items
 *
 * @param menuTree 菜單樹（已過濾，不含功能點）
 * @returns Ant Design Menu Items
 */
export function formatMenuTreeForAntd(menuTree: MenuItem[]): MenuProps['items'] {
  return menuTree.map(convertMenuItemToAntdItem);
}
