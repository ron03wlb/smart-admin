/**
 * Router Configuration
 * 路由配置
 *
 * 參考：Vue 版本 smart-admin-web/src/router/index.ts (166 行)
 * 遷移：Vue Router → React Router 7
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { createBrowserRouter, Navigate } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { Spin } from 'antd';
import ProtectedRoute from '@/components/ProtectedRoute';

// ==================== 懶加載組件 ====================

/**
 * 懶加載包裝器
 * 提供加載中狀態
 */
const LazyLoad = ({ children }: { children: React.ReactNode }) => {
  return <Suspense fallback={<Spin size="large" style={{ marginTop: '20%', display: 'block' }} />}>{children}</Suspense>;
};

// ==================== 頁面組件懶加載 ====================

/**
 * 登錄頁面
 * Vue: smart-admin-web/src/views/system/login/login.vue
 */
const LoginPage = lazy(() => import('@/views/system/login'));

/**
 * 首頁
 * Vue: smart-admin-web/src/views/home/home.vue
 */
const HomePage = lazy(() => import('@/views/home'));

/**
 * 基礎佈局
 */
const BasicLayout = lazy(() => import('@/layouts/BasicLayout'));

// ==================== 路由配置 ====================

/**
 * 路由配置
 * 使用 React Router 7 的 createBrowserRouter
 */
export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <LazyLoad>
        <LoginPage />
      </LazyLoad>
    ),
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <LazyLoad>
          <BasicLayout />
        </LazyLoad>
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <Navigate to="/home" replace />,
      },
      {
        path: 'home',
        element: (
          <LazyLoad>
            <HomePage />
          </LazyLoad>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/home" replace />,
  },
]);
