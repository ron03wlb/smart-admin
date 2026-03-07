/**
 * MainLayout Main Layout Component
 *
 * Features:
 * 1. Integrates Header, Sidebar, Breadcrumb, TagNav components
 * 2. Uses Ant Design Layout for nested layout
 * 3. Keep-Alive via CSS display:none (KeepAliveOutlet)
 * 4. Responsive design
 *
 * Layout structure:
 * <Layout>
 *   <Sidebar />
 *   <Layout>
 *     <Header />
 *     <Breadcrumb />
 *     <TagNav />
 *     <Content>
 *       <KeepAliveOutlet />
 *     </Content>
 *   </Layout>
 * </Layout>
 */

import React from 'react';
import { Layout } from 'antd';
import Header from './Header';
import Sidebar from './Sidebar';
import Breadcrumb from './Breadcrumb';
import TagNav from '@/components/layout/TagNav';
import KeepAliveOutlet from './KeepAliveOutlet';
import styles from './MainLayout.module.css';

const { Content } = Layout;

const MainLayout: React.FC = () => {
  return (
    <Layout className={styles.mainLayoutContainer} data-testid="main-layout">
      {/* Sidebar menu */}
      <Sidebar />

      {/* Right content area */}
      <Layout className={styles.mainLayoutContentWrapper}>
        {/* Top navigation */}
        <Header />

        {/* Breadcrumb */}
        <Breadcrumb />

        {/* Multi-tab navigation */}
        <TagNav />

        {/* Content area with Keep-Alive */}
        <Content className={styles.mainLayoutContent}>
          <div className={styles.mainLayoutContentInner}>
            <KeepAliveOutlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
