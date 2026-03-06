/**
 * Breadcrumb 麵包屑組件
 *
 * 功能：
 * 1. 根據當前路由自動生成麵包屑
 * 2. 麵包屑項點擊可跳轉（除最後一項）
 * 3. 首頁顯示 HomeOutlined 圖標
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */
import React from 'react';
import { Breadcrumb as AntBreadcrumb } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { useBreadcrumb } from '@/hooks/useBreadcrumb';
import styles from './Breadcrumb.module.css';

/**
 * Breadcrumb 麵包屑組件
 */
const Breadcrumb: React.FC = () => {
  const { breadcrumbItems, handleBreadcrumbClick } = useBreadcrumb();

  /**
   * 轉換為 Ant Design Breadcrumb items 格式
   */
  const antdBreadcrumbItems = breadcrumbItems.map((item, index) => {
    const isLast = index === breadcrumbItems.length - 1;

    return {
      title: (
        <span
          className={isLast ? styles.breadcrumbItemCurrent : styles.breadcrumbItemLink}
          onClick={!isLast && item.path ? () => handleBreadcrumbClick(item.path!) : undefined}
        >
          {item.isHome && <span className={styles.breadcrumbItemIcon}><HomeOutlined /></span>}
          {item.title}
        </span>
      ),
    };
  });

  return (
    <div className={styles.breadcrumbContainer} data-testid="breadcrumb">
      <AntBreadcrumb items={antdBreadcrumbItems} />
    </div>
  );
};

export default Breadcrumb;
