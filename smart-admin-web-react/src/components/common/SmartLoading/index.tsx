/**
 * SmartLoading Component
 * 全局加載指示器組件
 *
 * 參考：Vue 版本 smart-admin-web/src/components/framework/smart-loading/index.ts
 *
 * 用法：
 * 1. 在 App.tsx 中添加此組件：
 *    <SmartLoadingComponent />
 * 2. 在業務代碼中使用：
 *    import { SmartLoading } from '@/utils/SmartLoading';
 *    SmartLoading.show();
 *    SmartLoading.hide();
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import React from 'react';
import { Spin } from 'antd';
import { useAppSelector } from '@/hooks/useRedux';
import { selectLoading } from '@/store/slices/spinSlice';
import './index.css';

/**
 * 全局加載指示器組件
 * 注意：此組件應該在 App.tsx 根組件中引入
 */
const SmartLoadingComponent: React.FC = () => {
  const loading = useAppSelector(selectLoading);

  if (!loading) {
    return null;
  }

  return (
    <div className="smart-loading-container">
      <Spin size="large" />
    </div>
  );
};

export default SmartLoadingComponent;
