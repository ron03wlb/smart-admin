/**
 * 測試輔助工具
 * Test Utilities for React Component Testing
 *
 * @description 統一的測試輔助函數,用於簡化測試代碼並確保一致性
 * @author SmartAdmin React Team
 * @date 2026-03-20
 */

import { configureStore } from '@reduxjs/toolkit';
import { render, RenderOptions } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { ReactElement } from 'react';
import userReducer from '@/store/slices/userSlice';
import dictReducer from '@/store/slices/dictSlice';
import roleReducer from '@/store/slices/roleSlice';
import spinReducer from '@/store/slices/spinSlice';
import tagNavReducer from '@/store/slices/tagNavSlice';
import tenantReducer from '@/store/slices/tenantSlice';
import type { PermissionPoint } from '@/types/menu';

/**
 * 創建測試用的 Redux Store
 *
 * @param permissions 權限列表 (可選)
 * @param options 其他配置選項
 * @returns Redux Store 實例
 */
export function createTestStore(
  permissions: string[] = [],
  options?: {
    administratorFlag?: boolean;
    userInfo?: any;
    dictData?: Record<string, any[]>;
  }
) {
  const { administratorFlag = false, userInfo, dictData = {} } = options || {};

  // 將權限字符串轉換為 PermissionPoint 對象
  const pointsList: PermissionPoint[] = permissions.map((perm, index) => ({
    webPerms: perm,
    menuId: index + 1,
  }));

  return configureStore({
    reducer: {
      user: userReducer,
      dict: dictReducer,
      role: roleReducer,
      spin: spinReducer,
      tagNav: tagNavReducer,
      tenant: tenantReducer,
    },
    preloadedState: {
      user: {
        token: 'mock-token',
        employeeId: '1',
        employeeName: userInfo?.actualName || '管理員',
        loginName: userInfo?.loginName || 'admin',
        administratorFlag,
        menuTree: [],
        displayMenuTree: [],
        pointsList,
        menuRouterList: [],
        menuParentIdListMap: {},
        unreadMessageCount: 0,
        loading: false,
        error: null,
      },
      dict: {
        dictList: [],
        dictMap: dictData || {},
        loading: false,
        error: null,
        lastUpdated: null,
      },
      role: {
        checkedData: [],
        treeMap: {},
      },
      spin: {
        loading: false,
      },
      tagNav: {
        tags: [],
        activeTagPath: '',
        keepAliveEnabled: true,
        cachedPaths: [],
      },
      tenant: {
        tenantId: null,
        timezone: 'Asia/Taipei',
        tenantCode: 'default',
      },
    },
  });
}

/**
 * 自定義渲染函數,自動包裝 Redux Provider 和 Router
 *
 * @param ui React 組件
 * @param permissions 權限列表
 * @param options 渲染選項
 * @returns render 結果
 */
export function renderWithProviders(
  ui: ReactElement,
  {
    permissions = [],
    administratorFlag = false,
    userInfo,
    dictData,
    ...renderOptions
  }: {
    permissions?: string[];
    administratorFlag?: boolean;
    userInfo?: any;
    dictData?: Record<string, any[]>;
  } & Omit<RenderOptions, 'wrapper'> = {}
) {
  const store = createTestStore(permissions, { administratorFlag, userInfo, dictData });

  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <Provider store={store}>
        <BrowserRouter>{children}</BrowserRouter>
      </Provider>
    );
  }

  return { store, ...render(ui, { wrapper: Wrapper, ...renderOptions }) };
}

/**
 * 等待 Modal 出現並查找按鈕
 *
 * @param buttonText 按鈕文本 (支持正則表達式)
 * @returns 找到的按鈕元素
 */
export async function waitForModalButton(buttonText: string | RegExp) {
  const { screen, waitFor } = await import('@testing-library/react');

  return waitFor(() => {
    const buttons = screen.getAllByRole('button', { name: buttonText });
    if (buttons.length === 0) {
      throw new Error(`No button found with text: ${buttonText}`);
    }
    // 返回最後一個按鈕 (通常是 Modal 中的按鈕)
    return buttons[buttons.length - 1];
  });
}

/**
 * 等待並點擊 Modal 確認按鈕
 *
 * @description 處理 Ant Design Modal.confirm 的確認操作
 */
export async function confirmModal() {
  const { screen, waitFor, fireEvent } = await import('@testing-library/react');

  await waitFor(async () => {
    // Ant Design Modal.confirm 通常使用 "確定" 或 "OK" 作為確認按鈕文本
    const confirmButtons = screen.queryAllByRole('button', { name: /確定|OK|刪除/i });
    if (confirmButtons.length > 0) {
      // 點擊最後一個匹配的按鈕 (Modal 按鈕)
      fireEvent.click(confirmButtons[confirmButtons.length - 1]);
      // 等待一小段時間讓 async onOk 執行
      await new Promise(resolve => setTimeout(resolve, 100));
    }
  });
}

/**
 * 等待並點擊 Modal 取消按鈕
 */
export async function cancelModal() {
  const { screen, waitFor, fireEvent } = await import('@testing-library/react');

  await waitFor(() => {
    const cancelButtons = screen.getAllByRole('button', { name: /取消|Cancel/i });
    if (cancelButtons.length > 0) {
      fireEvent.click(cancelButtons[cancelButtons.length - 1]);
    }
  });
}

/**
 * 創建帶有完整 Mock 的 API 響應
 *
 * @param data 響應數據
 * @returns ResponseDTO 格式的 Mock 響應
 */
export function createMockResponse<T>(data: T) {
  return {
    code: 200,
    ok: true,
    msg: 'Success',
    data,
  };
}

/**
 * 創建帶有分頁的 Mock 響應
 *
 * @param list 數據列表
 * @param total 總數
 * @returns PageResult 格式的 Mock 響應
 */
export function createMockPageResponse<T>(list: T[], total?: number) {
  return createMockResponse({
    list,
    total: total ?? list.length,
    pageNum: 1,
    pageSize: 10,
    pages: 1,
    emptyFlag: list.length === 0,
  });
}

/**
 * 常用權限組合
 */
export const PERMISSIONS = {
  /** 所有 CRUD 權限 */
  ALL_CRUD: (module: string) => [
    `${module}:query`,
    `${module}:add`,
    `${module}:update`,
    `${module}:delete`,
    `${module}:batchDelete`,
  ],

  /** 只讀權限 */
  READ_ONLY: (module: string) => [`${module}:query`],

  /** CRUD + 導入導出權限 */
  FULL_ACCESS: (module: string) => [
    `${module}:query`,
    `${module}:add`,
    `${module}:update`,
    `${module}:delete`,
    `${module}:batchDelete`,
    `${module}:import`,
    `${module}:export`,
  ],
};
