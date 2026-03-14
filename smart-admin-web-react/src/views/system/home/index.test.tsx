/**
 * Home Page Tests
 * 首頁測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import HomePage from './index';
import userReducer from '@/store/slices/userSlice';

// Mock APIs
vi.mock('@/api/business/noticeApi', () => ({
  noticeApi: {
    queryEmployeeNotice: vi.fn().mockResolvedValue({
      data: { list: [] },
    }),
  },
}));

vi.mock('@/api/support/changeLogApi', () => ({
  changeLogApi: {
    queryPage: vi.fn().mockResolvedValue({
      data: { list: [] },
    }),
  },
}));

describe('HomePage', () => {
  let store: any;

  beforeEach(() => {
    vi.clearAllMocks();

    // Create mock store
    store = configureStore({
      reducer: {
        user: userReducer,
      },
      preloadedState: {
        user: {
          userInfo: {
            employeeId: 1,
            loginName: 'admin',
            actualName: '管理員',
            departmentName: '技術部',
            lastLoginTime: '2026-03-14 10:00:00',
            lastLoginIp: '192.168.1.1',
          },
          unreadMessageCount: 0,
        },
      },
    });
  });

  it('應該正確渲染首頁', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <HomePage />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      // 檢查是否有歡迎語（早上好、中午好等）
      const welcomeElement = screen.getByText(/好，管理員/);
      expect(welcomeElement).toBeInTheDocument();
    });
  });

  it('應該顯示所屬部門', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <HomePage />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText(/所屬部門：\s*技術部/)).toBeInTheDocument();
    });
  });

  it('應該顯示通知公告卡片', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <HomePage />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('公告')).toBeInTheDocument();
      expect(screen.getByText('通知')).toBeInTheDocument();
    });
  });

  it('應該顯示右側卡片', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <HomePage />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('聯繫我們')).toBeInTheDocument();
      expect(screen.getByText('更新日誌')).toBeInTheDocument();
      expect(screen.getByText('待辦工作')).toBeInTheDocument();
    });
  });
});
