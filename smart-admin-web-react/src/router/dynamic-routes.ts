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

  // ==================== Business 模塊 ====================
  '/business/goods': lazy(() => import('@/views/business/goods')),

  // ==================== Support 模塊 ====================
  '/support/file': lazy(() => import('@/views/support/file')),

  // 更多路由映射將在實現對應頁面時添加...
  // 例如：'/system/department', '/business/category', '/support/feedback' 等
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
