/**
 * Account Center Index
 *
 * Corresponds to Vue's system/account/index.vue (105L)
 * Tab-based container for personal center sub-pages.
 */
import React, { useState } from 'react';
import { Card, Menu } from 'antd';
import {
  UserOutlined,
  LockOutlined,
  MailOutlined,
  FileTextOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import AccountCenter from './components/AccountCenter';
import AccountPassword from './components/AccountPassword';
import AccountMessage from './components/AccountMessage';
import AccountLoginLog from './components/AccountLoginLog';
import AccountOperateLog from './components/AccountOperateLog';

const MENU_ITEMS = [
  { key: 'center', label: '个人中心', icon: <UserOutlined />, component: AccountCenter },
  { key: 'password', label: '修改密码', icon: <LockOutlined />, component: AccountPassword },
  { key: 'message', label: '我的消息', icon: <MailOutlined />, component: AccountMessage },
  { key: 'loginLog', label: '登录日志', icon: <HistoryOutlined />, component: AccountLoginLog },
  { key: 'operateLog', label: '操作日志', icon: <FileTextOutlined />, component: AccountOperateLog },
];

const AccountIndex: React.FC = () => {
  const [activeKey, setActiveKey] = useState('center');

  const ActiveComponent = MENU_ITEMS.find((m) => m.key === activeKey)?.component || AccountCenter;

  return (
    <div style={{ display: 'flex', gap: 16, height: '100%' }}>
      <Card style={{ width: 200, flexShrink: 0 }} styles={{ body: { padding: 0 } }}>
        <Menu
          mode="inline"
          selectedKeys={[activeKey]}
          onSelect={({ key }) => setActiveKey(key)}
          style={{ borderRight: 'none' }}
          items={MENU_ITEMS.map(({ key, label, icon }) => ({ key, label, icon }))}
        />
      </Card>
      <Card style={{ flex: 1, overflow: 'auto' }}>
        <ActiveComponent />
      </Card>
    </div>
  );
};

export default AccountIndex;
