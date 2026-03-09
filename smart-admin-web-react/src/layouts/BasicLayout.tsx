/**
 * BasicLayout Component
 * 基礎佈局組件（頂部導航 + 側邊欄 + 內容區域）
 *
 * 參考：Vue 版本 smart-admin-web/src/layout/index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useState } from 'react';
import { Layout, Menu, Button, Avatar, Dropdown } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectUserInfo, logout } from '@/store/slices/userSlice';

const { Header, Sider, Content } = Layout;

/**
 * 基礎佈局組件
 * 提供應用的整體佈局結構
 */
export default function BasicLayout() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  // Redux State
  const userInfo = useAppSelector(selectUserInfo);
  // TODO: Week 2 Day 4 - 動態菜單從 menuTree 生成
  // const menuTree = useAppSelector(selectMenuTree);

  // Local State
  const [collapsed, setCollapsed] = useState(false);

  /**
   * 處理菜單點擊
   */
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  /**
   * 處理退出登錄
   */
  const handleLogout = async () => {
    await dispatch(logout());
    navigate('/login');
  };

  /**
   * 用戶下拉菜單
   */
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登錄',
      onClick: handleLogout,
    },
  ];

  /**
   * 側邊欄菜單項（默認首頁）
   * TODO: 後續從 Redux menuTree 動態生成
   */
  const menuItems: MenuProps['items'] = [
    {
      key: '/home',
      icon: <HomeOutlined />,
      label: '首頁',
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* 左側邊欄 */}
      <Sider trigger={null} collapsible collapsed={collapsed} theme="dark">
        {/* Logo 區域 */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: 20,
            fontWeight: 'bold',
          }}
        >
          {collapsed ? 'SA' : 'SmartAdmin'}
        </div>

        {/* 菜單 */}
        <Menu
          theme="dark"
          mode="inline"
          defaultSelectedKeys={['/home']}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      {/* 右側內容區域 */}
      <Layout>
        {/* 頂部導航欄 */}
        <Header style={{ padding: '0 16px', background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          {/* 左側：折疊按鈕 */}
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: 16, width: 64, height: 64 }}
          />

          {/* 右側：用戶信息 */}
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} style={{ marginRight: 8 }} />
              <span>{userInfo.employeeName || userInfo.loginName || '用戶'}</span>
            </div>
          </Dropdown>
        </Header>

        {/* 主內容區域 */}
        <Content
          style={{
            margin: '16px',
            padding: 24,
            minHeight: 280,
            background: '#fff',
            borderRadius: 8,
          }}
        >
          {/* 渲染子路由 */}
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
