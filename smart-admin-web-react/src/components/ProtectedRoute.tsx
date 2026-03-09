/**
 * ProtectedRoute Component
 * 受保護路由組件（路由守衛）
 *
 * 功能：
 * - 檢查用戶是否已登錄（Token 驗證）
 * - 未登錄用戶自動跳轉登錄頁
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Navigate } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import { selectToken } from '@/store/slices/userSlice';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

/**
 * 受保護路由組件
 * 未登錄用戶將被重定向到登錄頁
 *
 * @example
 * ```tsx
 * <ProtectedRoute>
 *   <BasicLayout />
 * </ProtectedRoute>
 * ```
 */
export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const token = useAppSelector(selectToken);

  // 未登錄：跳轉登錄頁
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // 已登錄：渲染子組件
  return <>{children}</>;
}
