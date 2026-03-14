/**
 * Message List Page Tests
 * 消息管理列表頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import MessageListPage from './index';
import { messageApi } from '@/api/support/messageApi';

// Mock messageApi
vi.mock('@/api/support/messageApi', () => ({
  messageApi: {
    queryAdminMessage: vi.fn(),
    deleteMessage: vi.fn(),
  },
}));

// Mock employeeApi for MessageReceiverModal
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    queryEmployee: vi.fn(),
  },
}));

describe('MessageListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock default response
    vi.mocked(messageApi.queryAdminMessage).mockResolvedValue({
      code: 200,
      data: {
        list: [
          {
            messageId: 1,
            messageType: 1,
            title: '測試消息',
            content: '測試內容',
            receiverUserId: 100,
            receiverUserType: 1,
            readFlag: false,
            createTime: '2026-03-14 10:00:00',
            updateTime: '2026-03-14 10:00:00',
          },
        ],
        total: 1,
        pageNum: 1,
        pageSize: 10,
      },
      ok: true,
      msg: '',
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('應該正確渲染消息列表頁面', async () => {
    render(
      <BrowserRouter>
        <MessageListPage />
      </BrowserRouter>
    );

    // Wait for the page to load
    await waitFor(() => {
      expect(screen.getByText('查詢')).toBeInTheDocument();
    });

    // Check if the query button exists
    expect(screen.getByText('查詢')).toBeInTheDocument();
    expect(screen.getByText('重置')).toBeInTheDocument();
    expect(screen.getByText('發送消息')).toBeInTheDocument();
  });

  it('應該在初始加載時調用查詢 API', async () => {
    render(
      <BrowserRouter>
        <MessageListPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(messageApi.queryAdminMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          pageNum: 1,
          pageSize: 10,
        })
      );
    });
  });

  it('應該顯示查詢到的消息列表', async () => {
    render(
      <BrowserRouter>
        <MessageListPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('測試消息')).toBeInTheDocument();
      expect(screen.getByText('測試內容')).toBeInTheDocument();
    });
  });

  it('點擊重置按鈕應該重置查詢表單', async () => {
    render(
      <BrowserRouter>
        <MessageListPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('重置')).toBeInTheDocument();
    });

    const resetButton = screen.getByText('重置');
    fireEvent.click(resetButton);

    await waitFor(() => {
      // Should call API again after reset
      expect(messageApi.queryAdminMessage).toHaveBeenCalledTimes(2);
    });
  });

  it('應該顯示已讀狀態為「未讀」', async () => {
    render(
      <BrowserRouter>
        <MessageListPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('未讀')).toBeInTheDocument();
    });
  });
});
