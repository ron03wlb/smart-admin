/**
 * Dynamic Routes
 * 動態路由加載（基於菜單配置）
 *
 * 參考：Vue 版本 smart-admin-web/src/router/index.ts
 * 遷移：Vue 動態導入轉換為 React.lazy() 映射表
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { lazy, ComponentType } from 'react';

// ==================== 視圖組件映射表 ====================

/**
 * 視圖組件映射表
 * Vue 使用 import.meta.glob 自動掃描組件
 * React 使用手動映射 path 到 lazy component
 *
 * 說明：
 * - 鍵名為菜單路徑（例如：'/system/employee'）
 * - 值為 lazy 組件工廠函數
 * - 後續可以通過腳本自動生成此映射表
 */
export const viewModules: Record<string, ComponentType<any>> = {
  // ==================== System 模塊 ====================
  '/system/employee': lazy(() => import('@/views/system/employee')),
  '/system/role': lazy(() => import('@/views/system/role')),
  '/system/menu': lazy(() => import('@/views/system/menu')),
  '/system/position': lazy(() => import('@/views/system/position')),
  '/system/department': lazy(() => import('@/views/system/department')),
  '/system/account': lazy(() => import('@/views/system/account')),

  // ==================== Business 模塊 ====================
  '/business/goods': lazy(() => import('@/views/business/goods')),
  '/business/enterprise': lazy(() => import('@/views/business/enterprise')),
  '/business/notice': lazy(() => import('@/views/business/notice')),
  '/business/category': lazy(() => import('@/views/business/category')),

  // ==================== Support 模塊 ====================
  '/support/file': lazy(() => import('@/views/support/file')),
  '/support/config': lazy(() => import('@/views/support/config')),
  '/support/feedback': lazy(() => import('@/views/support/feedback')),
  '/support/login-log': lazy(() => import('@/views/support/login-log')),
  '/support/login-fail': lazy(() => import('@/views/support/login-fail')),
  '/support/operate-log': lazy(() => import('@/views/support/operate-log')),
  '/support/change-log': lazy(() => import('@/views/support/change-log')),
  '/support/job': lazy(() => import('@/views/support/job')),
  '/support/serial-number': lazy(() => import('@/views/support/serial-number')),
  '/support/dict': lazy(() => import('@/views/support/dict')),
  '/support/help-doc': lazy(() => import('@/views/support/help-doc')),
  '/support/cache': lazy(() => import('@/views/support/cache')),
  '/support/heart-beat': lazy(() => import('@/views/support/heart-beat')),
  '/support/reload': lazy(() => import('@/views/support/reload')),
  '/support/api-encrypt': lazy(() => import('@/views/support/api-encrypt')),
  '/support/message': lazy(() => import('@/views/support/message')),
  '/support/level3-protect': lazy(() => import('@/views/support/level3-protect')),
  '/support/level3-protect/data-masking': lazy(() => import('@/views/support/level3-protect/data-masking')),

  // 更多路由映射將在實現對應頁面時添加...
  // 例如：'/business/notice', '/business/category' 等
};

// ==================== 動態路由生成 ====================

/**
 * 根據路徑獲取對應的組件
 *
 * @param path 路由路徑
 * @returns 對應的 React 組件（lazy loaded）或 undefined
 */
export function getComponentByPath(path: string): ComponentType<any> | undefined {
  return viewModules[path];
}

/**
 * 檢查路徑是否已註冊
 *
 * @param path 路由路徑
 * @returns 是否存在對應組件
 */
export function hasRoute(path: string): boolean {
  return path in viewModules;
}

/**
 * 獲取所有已註冊的路由路徑
 *
 * @returns 所有路由路徑數組
 */
export function getAllRoutePaths(): string[] {
  return Object.keys(viewModules);
}
