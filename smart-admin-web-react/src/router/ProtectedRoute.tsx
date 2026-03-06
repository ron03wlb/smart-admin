/**
 * 路由保護組件
 *
 * 未登錄用戶訪問受保護路由時，重定向到登錄頁
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { Navigate } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import { selectIsLoggedIn } from '@/store/slices/userSlice';

interface ProtectedRouteProps {
  /**
   * 子組件（受保護的頁面）
   */
  children: React.ReactNode;

  /**
   * 重定向路徑（默認 '/'）
   */
  redirectTo?: string;
}

/**
 * 路由守衛組件（對應 Vue router.beforeEach）
 *
 * @example
 * <ProtectedRoute>
 *   <HomePage />
 * </ProtectedRoute>
 */
export default function ProtectedRoute({
  children,
  redirectTo = '/',
}: ProtectedRouteProps) {
  const isLoggedIn = useAppSelector(selectIsLoggedIn);

  // 未登錄則重定向到登錄頁
  if (!isLoggedIn) {
    return <Navigate to={redirectTo} replace />;
  }

  // 已登錄則渲染子組件
  return <>{children}</>;
}
