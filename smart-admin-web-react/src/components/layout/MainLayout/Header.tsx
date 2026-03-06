/**
 * Header 頂部導航組件
 *
 * 功能：
 * 1. Logo 顯示（點擊跳轉首頁）
 * 2. 全局搜尋（搜尋菜單、功能）
 * 3. 通知中心（Badge 顯示未讀數量）
 * 4. 用戶下拉菜單（顯示用戶名、退出登錄）
 *
 * 使用場景：
 * - MainLayout 頂部導航欄
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import {
  Layout,
  Input,
  Badge,
  Dropdown,
  Avatar,
  Space,
  message,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  SearchOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { selectEmployeeName, logout } from '@/store/slices/userSlice';
import styles from './Header.module.css';

const { Header: AntHeader } = Layout;

/**
 * Header 頂部導航組件
 *
 * @returns React Element
 */
const Header: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const employeeName = useSelector(selectEmployeeName);

  // 通知未讀數量（示例數據，實際應從 Redux Store 或 API 獲取）
  const [notificationCount] = useState(5);

  /**
   * 處理 Logo 點擊（跳轉首頁）
   */
  const handleLogoClick = (): void => {
    navigate('/home');
  };

  /**
   * 處理搜尋
   */
  const handleSearch = (value: string): void => {
    if (!value.trim()) {
      return;
    }
    // TODO: 實現全局搜尋邏輯
    message.info(`搜尋: ${value}`);
  };

  /**
   * 處理通知點擊
   */
  const handleNotificationClick = (): void => {
    // TODO: 實現通知中心邏輯
    message.info('通知中心開發中...');
  };

  /**
   * 處理退出登錄
   */
  const handleLogout = (): void => {
    dispatch(logout());
    navigate('/');
    message.success('已退出登錄');
  };

  /**
   * 用戶下拉菜單項
   */
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '個人資料',
      onClick: () => {
        // TODO: 實現個人資料頁面
        message.info('個人資料頁面開發中...');
      },
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '系統設置',
      onClick: () => {
        // TODO: 實現系統設置頁面
        message.info('系統設置頁面開發中...');
      },
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登錄',
      onClick: handleLogout,
    },
  ];

  return (
    <AntHeader className={styles.headerContainer}>
      {/* Logo */}
      <div className={styles.headerLogo} onClick={handleLogoClick}>
        <span className={styles.headerLogoText}>SmartAdmin</span>
      </div>

      {/* 全局搜尋 */}
      <div className={styles.headerSearch}>
        <Input
          placeholder="搜尋菜單、功能..."
          prefix={<SearchOutlined />}
          onPressEnter={(e) => handleSearch(e.currentTarget.value)}
          allowClear
          style={{ width: 300 }}
        />
      </div>

      {/* 右側工具欄 */}
      <div className={styles.headerActions}>
        <Space size="middle">
          {/* 通知中心 */}
          <Badge count={notificationCount} overflowCount={99}>
            <BellOutlined
              className={styles.headerActionIcon}
              onClick={handleNotificationClick}
            />
          </Badge>

          {/* 用戶下拉菜單 */}
          <Dropdown
            menu={{ items: userMenuItems }}
            placement="bottomRight"
            arrow
          >
            <div className={styles.headerUserMenu}>
              <Avatar size="small" icon={<UserOutlined />} />
              <span className={styles.headerUserName}>{employeeName || '用戶'}</span>
            </div>
          </Dropdown>
        </Space>
      </div>
    </AntHeader>
  );
};

export default Header;
