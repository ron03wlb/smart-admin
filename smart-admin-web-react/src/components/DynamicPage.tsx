/**
 * Dynamic Page Component
 * 動態頁面組件（根據路由路徑渲染對應組件）
 *
 * 說明：React Router 不支持像 Vue Router 那樣動態添加路由，
 * 因此使用此組件根據當前路徑動態渲染對應的頁面組件。
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useLocation, Navigate } from 'react-router-dom';
import { Suspense } from 'react';
import { Spin, Result } from 'antd';
import { getComponentByPath } from '@/router/dynamic-routes';

/**
 * 動態頁面組件
 * 根據當前路由路徑渲染對應的組件
 */
export default function DynamicPage() {
  const location = useLocation();
  const Component = getComponentByPath(location.pathname);

  // 如果路徑未註冊，顯示 404 頁面
  if (!Component) {
    return (
      <Result
        status="404"
        title="404"
        subTitle="抱歉，您訪問的頁面不存在"
        extra={
          <Navigate to="/home" replace />
        }
      />
    );
  }

  // 渲染對應組件（帶加載狀態）
  return (
    <Suspense
      fallback={
        <div style={{ textAlign: 'center', marginTop: '20%' }}>
          <Spin size="large" />
        </div>
      }
    >
      <Component />
    </Suspense>
  );
}
