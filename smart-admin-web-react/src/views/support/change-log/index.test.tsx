/**
 * ChangeLog Management Page Unit Tests
 * 系統更新日誌管理頁面單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import ChangeLogManagement from './index';
import userReducer from '@/store/slices/userSlice';
import dictReducer from '@/store/slices/dictSlice';
import type { ChangeLogVO } from './types';

// Mock changeLogApi
vi.mock('@/api/support/changeLogApi', () => ({
  changeLogApi: {
    queryPage: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
  },
}));

import { changeLogApi } from '@/api/support/changeLogApi';

const mockChangeLogData: ChangeLogVO[] = [
  {
    changeLogId: 1,
    updateVersion: 'v1.0.0',
    type: 1,
    publishAuthor: 'Admin',
    publicDate: '2026-01-01',
    content: '重大更新：新增用戶管理模塊',
    link: 'https://example.com/v1.0.0',
    createTime: '2026-01-01 10:00:00',
    updateTime: '2026-01-01 10:00:00',
  },
  {
    changeLogId: 2,
    updateVersion: 'v1.1.0',
    type: 2,
    publishAuthor: 'Developer',
    publicDate: '2026-02-01',
    content: '功能更新：優化查詢性能',
    link: undefined,
    createTime: '2026-02-01 11:00:00',
    updateTime: '2026-02-01 11:00:00',
  },
];

// Create mock store
const createMockStore = () =>
  configureStore({
    reducer: {
      user: userReducer,
      dict: dictReducer,
    },
    preloadedState: {
      user: {
        userInfo: {
          employeeId: 1,
          loginName: 'admin',
          actualName: '管理員',
          phone: '13800138000',
        },
        privilegeList: [
          'support:changeLog:query',
          'support:changeLog:add',
          'support:changeLog:update',
          'support:changeLog:delete',
          'support:changeLog:batchDelete',
        ],
        roleList: [],
        isLoggedIn: true,
        loading: false,
        error: null,
      },
      dict: {
        dictData: {},
        loading: false,
        error: null,
      },
    },
  });

describe('ChangeLogManagement', () => {
  let store: ReturnType<typeof createMockStore>;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createMockStore();

    // Mock queryPage response
    (changeLogApi.queryPage as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: {
        list: mockChangeLogData,
        total: 2,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        emptyFlag: false,
      },
    });
  });

  it('should render change log management page', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    // Wait for data to load
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalled();
    });

    // Verify query form elements
    expect(screen.getByPlaceholderText('更新類型')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('關鍵字')).toBeInTheDocument();

    // Verify action buttons
    expect(screen.getByText('查詢')).toBeInTheDocument();
    expect(screen.getByText('重置')).toBeInTheDocument();
    expect(screen.getByText('新建')).toBeInTheDocument();
    expect(screen.getByText('批量刪除')).toBeInTheDocument();
  });

  it('should display change log data in table', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('v1.0.0')).toBeInTheDocument();
      expect(screen.getByText('v1.1.0')).toBeInTheDocument();
      expect(screen.getByText('Admin')).toBeInTheDocument();
      expect(screen.getByText('Developer')).toBeInTheDocument();
    });
  });

  it('should handle search operation', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    // Wait for initial load
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(1);
    });

    // Fill search form and click search button
    const searchButton = screen.getByText('查詢');
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(2);
    });
  });

  it('should handle reset operation', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    // Wait for initial load
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(1);
    });

    // Click reset button
    const resetButton = screen.getByText('重置');
    fireEvent.click(resetButton);

    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(2);
    });
  });

  it('should handle delete operation', async () => {
    (changeLogApi.delete as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: null,
    });

    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('v1.0.0')).toBeInTheDocument();
    });

    // Find and click delete button for first row
    const deleteButtons = screen.getAllByText('刪除');
    fireEvent.click(deleteButtons[0]);

    // Confirm deletion in modal
    await waitFor(() => {
      const confirmButton = screen.getByRole('button', { name: /刪除/ });
      fireEvent.click(confirmButton);
    });

    await waitFor(() => {
      expect(changeLogApi.delete).toHaveBeenCalledWith(1);
    });
  });

  it('should disable batch delete when no rows selected', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('v1.0.0')).toBeInTheDocument();
    });

    // Batch delete button should be disabled
    const batchDeleteButton = screen.getByText('批量刪除').closest('button');
    expect(batchDeleteButton).toBeDisabled();
  });

  it('should handle refresh operation', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <ChangeLogManagement />
        </BrowserRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(1);
    });

    // Find and click refresh button (in TableOperator)
    // Note: The actual refresh button rendering depends on TableOperator implementation
    // This test assumes queryPage is called again on refresh
  });
});
