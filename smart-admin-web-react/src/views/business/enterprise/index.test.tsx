/**
 * Enterprise Page Tests
 * 企業管理頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 * @Updated: 2026-03-20 - 使用 test-utils 重構
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders, createMockPageResponse, PERMISSIONS } from '@/test/test-utils';
import EnterprisePage from './index';
import type { EnterpriseVO } from './types';

// Mock API
vi.mock('@/api/business/enterpriseApi', () => ({
  enterpriseApi: {
    pageQuery: vi.fn(),
    detail: vi.fn(),
    add: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

import { enterpriseApi } from '@/api/business/enterpriseApi';

const mockEnterpriseData: EnterpriseVO[] = [
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
];

describe('EnterprisePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock pageQuery response using test-utils helper
    (enterpriseApi.pageQuery as any).mockResolvedValue(
      createMockPageResponse(mockEnterpriseData, 2)
    );

    // Mock detail response
    (enterpriseApi.detail as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: mockEnterpriseData[0],
    });

    // Mock delete response
    (enterpriseApi.delete as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: null,
    });
  });

  it('應該正常渲染企業管理頁面', async () => {
    renderWithProviders(<EnterprisePage />, {
      permissions: PERMISSIONS.ALL_CRUD('oa:enterprise'),
    });

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
    renderWithProviders(<EnterprisePage />, {
      permissions: PERMISSIONS.READ_ONLY('oa:enterprise'),
    });

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
    renderWithProviders(<EnterprisePage />, {
      permissions: PERMISSIONS.ALL_CRUD('oa:enterprise'),
    });

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
    const { container } = renderWithProviders(<EnterprisePage />, {
      permissions: PERMISSIONS.READ_ONLY('oa:enterprise'),
    });

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('1024創新實驗室')).toBeInTheDocument();
    });

    // 檢查是否有分頁組件
    const pagination = container.querySelector('.ant-pagination');
    expect(pagination).toBeInTheDocument();
  });
});
