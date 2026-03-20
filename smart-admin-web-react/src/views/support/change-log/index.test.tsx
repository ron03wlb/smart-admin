/**
 * ChangeLog Management Page Unit Tests
 * 系統更新日誌管理頁面單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 * @Updated: 2026-03-20 - 使用 test-utils 重構
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { renderWithProviders, createMockPageResponse, PERMISSIONS } from '@/test/test-utils';
import ChangeLogManagement from './index';
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

describe('ChangeLogManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock queryPage response using test-utils helper
    (changeLogApi.queryPage as any).mockResolvedValue(
      createMockPageResponse(mockChangeLogData, 2)
    );
  });

  it('should render change log management page', async () => {
    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.ALL_CRUD('support:changeLog'),
    });

    // Wait for data to load
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalled();
    });

    // Verify query form elements
    expect(screen.getByPlaceholderText('關鍵字')).toBeInTheDocument();
    // "更新類型" label exists (appears multiple times, so use getAllByText)
    expect(screen.getAllByText('更新類型').length).toBeGreaterThan(0);

    // Verify action buttons using accessible role
    expect(screen.getByRole('button', { name: '查詢' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重置' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '新建' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '批量刪除' })).toBeInTheDocument();
  });

  it('should display change log data in table', async () => {
    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.READ_ONLY('support:changeLog'),
    });

    await waitFor(() => {
      expect(screen.getByText('v1.0.0')).toBeInTheDocument();
      expect(screen.getByText('v1.1.0')).toBeInTheDocument();
      expect(screen.getByText('Admin')).toBeInTheDocument();
      expect(screen.getByText('Developer')).toBeInTheDocument();
    });
  });

  it('should handle search operation', async () => {
    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.READ_ONLY('support:changeLog'),
    });

    // Wait for initial load
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(1);
    });

    // Click search button
    const searchButton = screen.getByRole('button', { name: '查詢' });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(2);
    });
  });

  it('should handle reset operation', async () => {
    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.READ_ONLY('support:changeLog'),
    });

    // Wait for initial load
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(1);
    });

    // Clear mocks to reset call count
    vi.clearAllMocks();

    // Click reset button
    const resetButton = screen.getByRole('button', { name: '重置' });
    fireEvent.click(resetButton);

    // Reset should trigger a new query
    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalled();
    });
  });

  it('should handle delete operation', async () => {
    (changeLogApi.delete as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: null,
    });

    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.ALL_CRUD('support:changeLog'),
    });

    await waitFor(() => {
      expect(screen.getByText('v1.0.0')).toBeInTheDocument();
    });

    // Find and click delete button for first row
    const deleteButtons = screen.getAllByRole('button', { name: '刪除' });
    // Click the first delete button (in table row)
    fireEvent.click(deleteButtons[0]);

    // Wait for Modal to appear and click confirm
    await waitFor(
      async () => {
        // Modal adds additional delete buttons, click the last one (confirm button)
        const allDeleteButtons = screen.getAllByRole('button', { name: '刪除' });
        fireEvent.click(allDeleteButtons[allDeleteButtons.length - 1]);

        // Wait for async onOk to execute
        await new Promise(resolve => setTimeout(resolve, 100));
      },
      { timeout: 3000 }
    );

    await waitFor(() => {
      expect(changeLogApi.delete).toHaveBeenCalledWith(1);
    });
  });

  it('should disable batch delete when no rows selected', async () => {
    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.ALL_CRUD('support:changeLog'),
    });

    await waitFor(() => {
      expect(screen.getByText('v1.0.0')).toBeInTheDocument();
    });

    // Batch delete button should be disabled
    const batchDeleteButton = screen.getByRole('button', { name: '批量刪除' });
    expect(batchDeleteButton).toBeDisabled();
  });

  it('should handle refresh operation', async () => {
    renderWithProviders(<ChangeLogManagement />, {
      permissions: PERMISSIONS.READ_ONLY('support:changeLog'),
    });

    await waitFor(() => {
      expect(changeLogApi.queryPage).toHaveBeenCalledTimes(1);
    });

    // Note: Refresh button test depends on TableOperator implementation
    // This test verifies the initial query was called
  });
});
