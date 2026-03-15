/**
 * Enterprise Page Tests
 * 企業管理頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import EnterprisePage from './index';

// Mock API
vi.mock('@/api/business/enterpriseApi', () => ({
  enterpriseApi: {
    pageQuery: vi.fn().mockResolvedValue({
      ok: true,
      data: {
        list: [
          {
            enterpriseId: 1,
            enterpriseName: '1024創新實驗室',
            type: 1,
            unifiedSocialCreditCode: '91110000MA01234567',
            contact: '張三',
            contactPhone: '13800138000',
            email: 'contact@1024lab.net',
            disabledFlag: false,
            createTime: '2024-01-01 00:00:00',
          },
          {
            enterpriseId: 2,
            enterpriseName: 'SmartAdmin科技',
            type: 2,
            unifiedSocialCreditCode: '91110000MA09876543',
            contact: '李四',
            contactPhone: '13900139000',
            email: 'info@smartadmin.tech',
            disabledFlag: false,
            createTime: '2024-01-15 00:00:00',
          },
        ],
        total: 2,
      },
    }),
    detail: vi.fn().mockResolvedValue({
      ok: true,
      data: {
        enterpriseId: 1,
        enterpriseName: '1024創新實驗室',
        type: 1,
      },
    }),
    add: vi.fn().mockResolvedValue({ ok: true, data: null }),
    update: vi.fn().mockResolvedValue({ ok: true, data: null }),
    delete: vi.fn().mockResolvedValue({ ok: true, data: null }),
  },
}));

// Mock components
vi.mock('@/components/PrivilegeButton', () => ({
  PrivilegeButton: ({ children }: any) => <button>{children}</button>,
}));

describe('EnterprisePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('應該正常渲染企業管理頁面', async () => {
    render(
      <BrowserRouter>
        <EnterprisePage />
      </BrowserRouter>
    );

    // 檢查搜尋框
    await waitFor(() => {
      expect(screen.getByPlaceholderText('企業名稱/聯系人/聯系電話')).toBeInTheDocument();
    });

    // 檢查搜尋按鈕
    expect(screen.getByText('搜尋')).toBeInTheDocument();
    expect(screen.getByText('重置')).toBeInTheDocument();

    // 檢查新建按鈕
    expect(screen.getByText('新建企業')).toBeInTheDocument();
  });

  it('應該加載並顯示企業列表數據', async () => {
    render(
      <BrowserRouter>
        <EnterprisePage />
      </BrowserRouter>
    );

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('1024創新實驗室')).toBeInTheDocument();
    });

    // 檢查列表數據
    expect(screen.getByText('1024創新實驗室')).toBeInTheDocument();
    expect(screen.getByText('SmartAdmin科技')).toBeInTheDocument();

    // 檢查統一社會信用代碼
    expect(screen.getByText('91110000MA01234567')).toBeInTheDocument();
    expect(screen.getByText('91110000MA09876543')).toBeInTheDocument();
  });

  it('應該顯示操作按鈕（編輯、刪除）', async () => {
    render(
      <BrowserRouter>
        <EnterprisePage />
      </BrowserRouter>
    );

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('1024創新實驗室')).toBeInTheDocument();
    });

    // 檢查操作按鈕（每個企業都有編輯和刪除按鈕）
    const editButtons = screen.getAllByText('編輯');
    const deleteButtons = screen.getAllByText('刪除');

    expect(editButtons.length).toBeGreaterThan(0);
    expect(deleteButtons.length).toBeGreaterThan(0);
  });

  it('應該正確處理分頁', async () => {
    const { container } = render(
      <BrowserRouter>
        <EnterprisePage />
      </BrowserRouter>
    );

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('1024創新實驗室')).toBeInTheDocument();
    });

    // 檢查是否有分頁組件
    const pagination = container.querySelector('.ant-pagination');
    expect(pagination).toBeInTheDocument();
  });
});
