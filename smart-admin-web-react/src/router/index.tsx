/**
 * React Router 配置
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { createBrowserRouter } from 'react-router-dom';
import Login from '@/views/Login';
import Home from '@/views/Home';
import MainLayout from '@/components/layout/MainLayout';
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
    path: '/',
    element: (
      <ProtectedRoute>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        path: 'home',
        element: <Home />,
      },
      // 未來添加其他子路由
      // {
      //   path: 'player/list',
      //   element: <PlayerList />,
      // },
    ],
  },
  // 404 頁面（未來添加）
  // {
  //   path: '*',
  //   element: <NotFound />,
  // },
]);
