/**
 * Header 頂部導航組件
 *
 * 功能：
 * 1. Logo 顯示（點擊跳轉首頁）
 * 2. 全局搜尋（搜尋菜單、功能）
 * 3. 消息通知（HeaderMessage）
 * 4. 系統設置（HeaderSetting Drawer）
 * 5. 用戶頭像（HeaderAvatar + 修改密碼）
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Layout,
  Input,
  Space,
  message,
} from 'antd';
import {
  SearchOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import HeaderAvatar from './HeaderAvatar';
import HeaderMessage from './HeaderMessage';
import HeaderSetting from './HeaderSetting';
import styles from './Header.module.css';

const { Header: AntHeader } = Layout;

const Header: React.FC = () => {
  const navigate = useNavigate();
  const [settingVisible, setSettingVisible] = useState(false);

  const handleLogoClick = (): void => {
    navigate('/home');
  };

  const handleSearch = (value: string): void => {
    if (!value.trim()) return;
    message.info(`搜尋: ${value}`);
  };

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
          {/* 消息通知 */}
          <HeaderMessage />

          {/* 系統設置 */}
          <SettingOutlined
            className={styles.headerActionIcon}
            onClick={() => setSettingVisible(true)}
          />

          {/* 用戶頭像 */}
          <HeaderAvatar />
        </Space>
      </div>

      {/* 設置抽屜 */}
      <HeaderSetting visible={settingVisible} onClose={() => setSettingVisible(false)} />
    </AntHeader>
  );
};

export default Header;
