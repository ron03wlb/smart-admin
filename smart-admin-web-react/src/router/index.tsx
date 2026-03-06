/**
 * React Router 配置
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { createBrowserRouter } from 'react-router-dom';
import Login from '@/views/Login';
import Home from '@/views/Home';
import ProtectedRoute from './ProtectedRoute';

/**
 * 路由配置
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <Login />,
  },
  {
    path: '/home',
    element: (
      <ProtectedRoute>
        <Home />
      </ProtectedRoute>
    ),
  },
  // 404 頁面（未來添加）
  // {
  //   path: '*',
  //   element: <NotFound />,
  // },
]);
