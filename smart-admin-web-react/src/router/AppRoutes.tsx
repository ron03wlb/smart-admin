/**
 * 動態路由組件
 *
 * 從 Redux Store 讀取菜單列表，動態生成路由配置。
 * 替代原有的靜態 createBrowserRouter。
 */
import { useMemo } from 'react';
import { useRoutes, Navigate } from 'react-router-dom';
import type { RouteObject } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import { selectIsLoggedIn, selectMenuRouterList } from '@/store/slices/userSlice';
import { buildDynamicRoutes } from '@/utils/routeBuilder';
import Login from '@/views/Login';
import Home from '@/views/Home';
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from './ProtectedRoute';

/**
 * 應用路由組件
 *
 * 根據登入狀態和菜單數據動態生成路由
 */
export default function AppRoutes() {
  const isLoggedIn = useAppSelector(selectIsLoggedIn);
  const menuRouterList = useAppSelector(selectMenuRouterList);

  const routes: RouteObject[] = useMemo(() => {
    // 從後端菜單列表生成動態路由
    const dynamicRoutes = buildDynamicRoutes(menuRouterList);

    return [
      // 登入頁（公開）
      {
        path: '/login',
        element: isLoggedIn ? <Navigate to="/home" replace /> : <Login />,
      },

      // 受保護的路由（需要登入）
      {
        path: '/',
        element: (
          <ProtectedRoute redirectTo="/login">
            <MainLayout />
          </ProtectedRoute>
        ),
        children: [
          // 根路徑重定向到首頁
          { index: true, element: <Navigate to="/home" replace /> },
          // 首頁（靜態路由）
          { path: 'home', element: <Home /> },
          // 動態路由（從後端菜單生成）
          ...dynamicRoutes,
        ],
      },

      // 舊的根路徑登入頁兼容（重定向到 /login）
      {
        path: '/',
        element: isLoggedIn ? <Navigate to="/home" replace /> : <Navigate to="/login" replace />,
      },

      // 404
      {
        path: '*',
        element: isLoggedIn ? <Navigate to="/home" replace /> : <Navigate to="/login" replace />,
      },
    ];
  }, [isLoggedIn, menuRouterList]);

  return useRoutes(routes);
}
