/**
 * Header Avatar Component
 *
 * Corresponds to Vue's header-avatar.vue (143 lines)
 * User avatar with dropdown menu (profile, change password, logout).
 */
import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { Dropdown, Avatar } from 'antd';
import type { MenuProps } from 'antd';
import { UserOutlined, LockOutlined, LogoutOutlined } from '@ant-design/icons';
import { selectEmployeeName, logout } from '@/store/slices/userSlice';
import { AVATAR_BACKGROUND_COLORS, stringHashCode } from '@/theme/colors';
import ChangePasswordModal from './ChangePasswordModal';

const HeaderAvatar: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const employeeName = useSelector(selectEmployeeName);
  const [pwdModalVisible, setPwdModalVisible] = useState(false);

  const avatarName = useMemo(() => (employeeName ? employeeName.charAt(0) : ''), [employeeName]);

  const avatarColor = useMemo(() => {
    if (!avatarName) return AVATAR_BACKGROUND_COLORS[0];
    return AVATAR_BACKGROUND_COLORS[stringHashCode(avatarName) % AVATAR_BACKGROUND_COLORS.length];
  }, [avatarName]);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/');
  };

  const menuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/account'),
    },
    {
      key: 'password',
      icon: <LockOutlined />,
      label: '修改密码',
      onClick: () => setPwdModalVisible(true),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  return (
    <>
      <Dropdown menu={{ items: menuItems }} placement="bottomRight" arrow>
        <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}>
          <Avatar size="small" style={{ backgroundColor: avatarColor }}>
            {avatarName}
          </Avatar>
          <span style={{ fontWeight: 500 }}>{employeeName || '用户'}</span>
        </div>
      </Dropdown>
      <ChangePasswordModal visible={pwdModalVisible} onClose={() => setPwdModalVisible(false)} />
    </>
  );
};

export default HeaderAvatar;
