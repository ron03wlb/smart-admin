/**
 * MainLayout 主佈局組件
 *
 * 功能：
 * 1. 整合 Header、Sidebar、Breadcrumb 組件
 * 2. 使用 Ant Design Layout 構建嵌套佈局
 * 3. 提供 Outlet 用於子路由渲染
 * 4. 響應式設計（移動端自動適配）
 *
 * 佈局結構：
 * <Layout>                            // 外層容器
 *   <Sidebar />                       // 側邊菜單
 *   <Layout>                          // 右側主容器
 *     <Header />                      // 頂部導航
 *     <Breadcrumb />                  // 麵包屑
 *     <Content>                       // 內容區域
 *       <Outlet />                    // 子路由出口
 *     </Content>
 *   </Layout>
 * </Layout>
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import React from 'react';
import { Layout } from 'antd';
import { Outlet } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';
import Breadcrumb from './Breadcrumb';
import styles from './MainLayout.module.css';

const { Content } = Layout;

/**
 * MainLayout 主佈局組件
 */
const MainLayout: React.FC = () => {
  return (
    <Layout className={styles.mainLayoutContainer} data-testid="main-layout">
      {/* 側邊菜單 */}
      <Sidebar />

      {/* 右側主容器 */}
      <Layout className={styles.mainLayoutContentWrapper}>
        {/* 頂部導航 */}
        <Header />

        {/* 麵包屑 */}
        <Breadcrumb />

        {/* 內容區域 */}
        <Content className={styles.mainLayoutContent}>
          <div className={styles.mainLayoutContentInner}>
            {/* 子路由渲染出口 */}
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
