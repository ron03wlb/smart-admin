/**
 * KeepAlive Outlet Component
 * 支持頁面緩存的路由出口組件
 *
 * 基於 react-activation 實現頁面 Keep-alive 功能
 * 參考：Vue 版本 smart-admin-web/src/layout/components/smart-keep-alive.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { Outlet, useLocation } from 'react-router-dom';
import { KeepAlive } from 'react-activation';

/**
 * KeepAlive Outlet 組件
 *
 * 使用方式：
 * 在 BasicLayout 中替換 <Outlet /> 為 <KeepAliveOutlet />
 *
 * 功能：
 * - 自動緩存已訪問過的頁面
 * - 基於路由路徑生成唯一緩存 key
 * - 當路由變化時保持組件實例不銷毀
 */
export default function KeepAliveOutlet() {
  const location = useLocation();

  /**
   * 緩存 key 策略：
   * 使用 location.pathname 作為唯一標識
   * 確保相同路由使用相同的緩存實例
   */
  const cacheKey = location.pathname;

  return (
    <KeepAlive
      name={cacheKey}
      when={true} // 總是啟用緩存（後續可根據路由 meta 配置）
      saveScrollPosition={true} // 保存滾動位置
    >
      <Outlet />
    </KeepAlive>
  );
}
