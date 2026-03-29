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
import { selectIsLoggedIn, selectMenuTree } from '@/store/slices/userSlice';
import { buildDynamicRoutes } from '@/utils/routeBuilder';
import type { MenuItem } from '@/types/menu';

function flattenMenuTree(tree: MenuItem[]): MenuItem[] {
  const result: MenuItem[] = [];
  for (const item of tree) {
    result.push(item);
    if (item.children && item.children.length > 0) {
      result.push(...flattenMenuTree(item.children));
    }
  }
  return result;
}
import Login from '@/views/Login';
import Login2 from '@/views/Login2/Login2';
import Login3 from '@/views/Login3/Login3';
import Home from '@/views/home';
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from './ProtectedRoute';

/**
 * 應用路由組件
 *
 * 根據登入狀態和菜單數據動態生成路由
 */
export default function AppRoutes() {
  const isLoggedIn = useAppSelector(selectIsLoggedIn);
  const menuTree = useAppSelector(selectMenuTree);

  const routes: RouteObject[] = useMemo(() => {
    // 從後端菜單列表生成動態路由
    const dynamicRoutes = buildDynamicRoutes(flattenMenuTree(menuTree));

    return [
      // 登入頁（公開）
      {
        path: '/login',
        element: isLoggedIn ? <Navigate to="/home" replace /> : <Login />,
      },

      // Login 變體頁面（公開）
      {
        path: '/login2',
        element: isLoggedIn ? <Navigate to="/home" replace /> : <Login2 />,
      },
      {
        path: '/login3',
        element: isLoggedIn ? <Navigate to="/home" replace /> : <Login3 />,
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
  }, [isLoggedIn, menuTree]);

  return useRoutes(routes);
}
