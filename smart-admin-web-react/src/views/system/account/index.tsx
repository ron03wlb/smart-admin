/**
 * Account Page
 * 個人中心主頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/account/index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Menu, Badge, Spin } from 'antd';
import { getAccountMenuList, getAccountMenuById, type AccountMenuItem } from './accountMenu';
import { useAppSelector } from '@/store/hooks';
import './index.css';

const AccountPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const menuList = getAccountMenuList();
  const [selectedMenu, setSelectedMenu] = useState<AccountMenuItem>(menuList[0]);

  // 從 Redux 獲取未讀消息數
  const unreadMessageCount = useAppSelector((state) => state.user.unreadMessageCount || 0);

  /**
   * 初始化和路由參數處理
   */
  useEffect(() => {
    const menuId = searchParams.get('menuId');
    if (menuId) {
      const menu = getAccountMenuById(menuId);
      if (menu) {
        setSelectedMenu(menu);
      }
    }
  }, [searchParams]);

  /**
   * 選擇菜單
   */
  const handleSelectMenu = (menuId: string) => {
    const menu = getAccountMenuById(menuId);
    if (menu) {
      setSelectedMenu(menu);
      setSearchParams({ menuId });
    }
  };

  /**
   * 渲染菜單項標籤（帶未讀消息徽章）
   */
  const renderMenuLabel = (menu: AccountMenuItem) => {
    if (menu.menuId === 'message' && unreadMessageCount > 0) {
      return (
        <span>
          {menu.menuName}
          <Badge count={unreadMessageCount} style={{ marginLeft: 10 }} />
        </span>
      );
    }
    return menu.menuName;
  };

  /**
   * 菜單項配置
   */
  const menuItems = menuList.map((menu) => ({
    key: menu.menuId,
    label: renderMenuLabel(menu),
  }));

  /**
   * 動態渲染選中的組件
   */
  const SelectedComponent = selectedMenu.component;

  return (
    <div className="account-container">
      {/* 左側菜單 */}
      <div className="account-menu-list">
        <Menu
          mode="inline"
          selectedKeys={[selectedMenu.menuId]}
          items={menuItems}
          onClick={({ key }) => handleSelectMenu(key)}
        />
      </div>

      {/* 右側內容區 */}
      <div className="account-content">
        <Suspense fallback={<Spin size="large" style={{ display: 'block', margin: '100px auto' }} />}>
          <SelectedComponent />
        </Suspense>
      </div>
    </div>
  );
};

export default AccountPage;
