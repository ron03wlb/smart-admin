/**
 * 測試輔助工具
 *
 * 功能：
 * 1. renderWithProviders() - 統一的 React 渲染函數（with Redux Provider）
 * 2. 重新導出 Testing Library 工具（方便統一導入）
 *
 * 使用場景：
 * - 單元測試：需要 Redux Store 的組件測試
 * - 避免每個測試文件重複包裝 <Provider store={store}>
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { render } from '@testing-library/react';
import type { RenderOptions } from '@testing-library/react';
import { Provider } from 'react-redux';
import type { ReactElement } from 'react';
import type { UserState } from '@/types/user.types';
import { createTestStore } from './store-factory';
import type { TestStorePreloadedState } from './store-factory';

/**
 * 擴展的渲染選項
 */
interface ExtendedRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  /**
   * 預加載的 State（可選）
   *
   * @example
   * ```typescript
   * // 新的 API（推薦）- 支援 user 和 menu slice
   * { user: { administratorFlag: true }, menu: { collapsed: true } }
   *
   * // 舊的 API（兼容）- 只支援 UserState
   * { administratorFlag: true, pointsList: [...] }
   * ```
   */
  preloadedState?: TestStorePreloadedState | Partial<UserState>;

  /**
   * 自定義 Redux Store（可選，默認使用 createTestStore）
   *
   * @example
   * ```typescript
   * const customStore = createTestStore({ user: { token: 'custom-token' } });
   * renderWithProviders(<MyComponent />, { store: customStore });
   * ```
   */
  store?: ReturnType<typeof createTestStore>;
}

/**
 * 統一的 React 渲染函數（with Redux Provider）
 *
 * @param ui - React 組件
 * @param options - 渲染選項
 * @returns Testing Library render 結果 + store
 *
 * @example
 * ```typescript
 * // 基本用法
 * const { getByRole } = renderWithProviders(<MyButton />);
 *
 * // 自定義 preloadedState（新的 API）
 * const { getByRole, store } = renderWithProviders(
 *   <Sidebar />,
 *   {
 *     preloadedState: {
 *       user: { administratorFlag: true, menuTree: [...] },
 *       menu: { collapsed: true, selectedMenuId: '11' },
 *     },
 *   }
 * );
 *
 * // 舊的 API（兼容）
 * const { getByRole, store } = renderWithProviders(
 *   <PrivilegeButton permissionCode="system:user:add">新增</PrivilegeButton>,
 *   { preloadedState: { administratorFlag: true } }
 * );
 *
 * // 驗證 Redux 狀態
 * expect(store.getState().user.administratorFlag).toBe(true);
 * expect(store.getState().menu.collapsed).toBe(true);
 * ```
 */
export function renderWithProviders(
  ui: ReactElement,
  {
    preloadedState = {},
    store = createTestStore(preloadedState),
    ...renderOptions
  }: ExtendedRenderOptions = {}
) {
  /**
   * Wrapper 組件（包含 Redux Provider）
   */
  function Wrapper({ children }: { children: React.ReactNode }) {
    return <Provider store={store}>{children}</Provider>;
  }

  return {
    store,
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
  };
}

/**
 * 重新導出 Testing Library 工具（方便統一導入）
 *
 * @example
 * ```typescript
 * // ✅ 統一從 test-utils 導入
 * import { renderWithProviders, screen, waitFor } from '@/test/utils/test-utils';
 *
 * // ❌ 不需要分別從多個包導入
 * // import { render, screen, waitFor } from '@testing-library/react';
 * // import { Provider } from 'react-redux';
 * ```
 */
// eslint-disable-next-line react-refresh/only-export-components
export * from '@testing-library/react';
export { userEvent } from '@testing-library/user-event';
