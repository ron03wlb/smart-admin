/**
 * Ant Design 圖標映射
 *
 * 將後端菜單中的圖標名稱字串映射為 React 圖標組件。
 * 後端存儲的圖標名稱格式如 "UserOutlined"、"SettingOutlined"。
 */
import React from 'react';
import * as Icons from '@ant-design/icons';

/**
 * 根據圖標名稱字串獲取 Ant Design 圖標組件
 *
 * @param iconName 圖標名稱（如 "UserOutlined"、"SettingOutlined"）
 * @returns React 圖標元素，找不到則返回 undefined
 */
export function getIconByName(iconName?: string): React.ReactNode {
  if (!iconName) return undefined;

  const IconComponent = (Icons as Record<string, React.ComponentType>)[iconName];
  if (IconComponent) {
    return React.createElement(IconComponent);
  }

  return undefined;
}
