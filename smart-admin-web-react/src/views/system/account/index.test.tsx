/**
 * Account Page Tests
 * 個人中心頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import AccountPage from './index';
import userReducer from '@/store/slices/userSlice';

// Mock employeeApi
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    getEmployee: vi.fn(),
    updateEmployee: vi.fn(),
    getPasswordComplexityEnabled: vi.fn(),
    updateEmployeePassword: vi.fn(),
  },
}));

describe('AccountPage', () => {
  let store: any;

  beforeEach(() => {
    vi.clearAllMocks();

    // Create mock store
    store = configureStore({
      reducer: {
        user: userReducer as any,
      },
      preloadedState: {
        user: {
          userInfo: {
            employeeId: 1,
            loginName: 'admin',
            actualName: '管理員',
          },
          unreadMessageCount: 0,
        },
      },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('應該正確渲染個人中心頁面', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <AccountPage />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      // Check if menu items are rendered
      expect(screen.getByText('個人中心')).toBeInTheDocument();
      expect(screen.getByText('修改密碼')).toBeInTheDocument();
      expect(screen.getByText('我的消息')).toBeInTheDocument();
      expect(screen.getByText('通知公告')).toBeInTheDocument();
      expect(screen.getByText('登錄日誌')).toBeInTheDocument();
      expect(screen.getByText('操作日誌')).toBeInTheDocument();
      expect(screen.getByText('多因素認證')).toBeInTheDocument();
    });
  });

  it('應該顯示 7 個菜單項', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <AccountPage />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      const menuItems = screen.getAllByRole('menuitem');
      expect(menuItems.length).toBe(7);
    });
  });
});
