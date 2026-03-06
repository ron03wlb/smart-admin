/**
 * Sidebar 側邊菜單組件
 *
 * 功能：
 * 1. 基於 state.user.menuTree 渲染菜單樹
 * 2. 支援菜單折疊/展開（狀態存儲在 Redux Store + LocalStorage）
 * 3. 當前選中菜單高亮（基於路由）
 * 4. 菜單項點擊跳轉路由
 * 5. 自動展開父級菜單（根據當前路由）
 *
 * 使用場景：
 * - MainLayout 側邊菜單欄
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import React from 'react';
import { Layout, Menu } from 'antd';
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons';
import { useMenu } from '@/hooks/useMenu';
import styles from './Sidebar.module.css';

const { Sider } = Layout;

/**
 * Sidebar 側邊菜單組件
 *
 * @returns React Element
 */
const Sidebar: React.FC = () => {
  const {
    menuItems,
    collapsed,
    openKeys,
    selectedKeys,
    handleToggleCollapsed,
    handleOpenChange,
    handleMenuClick,
  } = useMenu();

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      onCollapse={handleToggleCollapsed}
      trigger={null}
      width={240}
      className={styles.sidebarContainer}
      data-testid="sidebar"
    >
      {/* 折疊觸發按鈕 */}
      <div className={styles.sidebarTrigger} onClick={handleToggleCollapsed} data-testid="sidebar-trigger">
        {collapsed ? (
          <MenuUnfoldOutlined className={styles.sidebarTriggerIcon} />
        ) : (
          <MenuFoldOutlined className={styles.sidebarTriggerIcon} />
        )}
      </div>

      {/* 菜單 */}
      <Menu
        mode="inline"
        theme="dark"
        items={menuItems}
        selectedKeys={selectedKeys}
        openKeys={collapsed ? [] : openKeys}
        onClick={({ key }) => handleMenuClick(key)}
        onOpenChange={handleOpenChange}
        className={styles.sidebarMenu}
      />
    </Sider>
  );
};

export default Sidebar;
