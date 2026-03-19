/**
 * Reload List Page Component Unit Tests
 * Reload 列表頁面組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import ReloadListPage from './index';
import { reloadApi } from '@/api/support/reloadApi';
import type { ReloadVO } from './types';

// Mock reloadApi
vi.mock('@/api/support/reloadApi', () => ({
  reloadApi: {
    queryList: vi.fn(),
    reload: vi.fn(),
    queryReloadResult: vi.fn(),
  },
}));

// Mock usePrivilege Hook
vi.mock('@/hooks/usePrivilege', () => ({
  usePrivilege: vi.fn((permission: string) => {
    // 默認所有權限都返回 true
    return true;
  }),
}));

const mockReloadData: ReloadVO[] = [
  {
    tag: 'ConfigReload',
    identification: 'config-v1',
    args: '{"cacheKey":"system_config"}',
    createTime: '2026-03-19 10:00:00',
    updateTime: '2026-03-19 10:30:00',
  },
  {
    tag: 'CacheReload',
    identification: 'cache-v2',
    args: '{"type":"redis"}',
    createTime: '2026-03-19 09:00:00',
    updateTime: '2026-03-19 09:15:00',
  },
];

describe('ReloadListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render reload list table with data', async () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockReloadData,
    });

    render(<ReloadListPage />);

    // Verify table title columns are displayed
    expect(screen.getByText('標籤')).toBeInTheDocument();
    expect(screen.getByText('運行標識')).toBeInTheDocument();
    expect(screen.getByText('參數')).toBeInTheDocument();
    expect(screen.getByText('更新時間')).toBeInTheDocument();
    expect(screen.getByText('創建時間')).toBeInTheDocument();
    expect(screen.getByText('操作')).toBeInTheDocument();

    // Wait for data to be loaded
    await waitFor(() => {
      expect(screen.getByText('ConfigReload')).toBeInTheDocument();
    });

    expect(screen.getByText('config-v1')).toBeInTheDocument();
    expect(screen.getByText('CacheReload')).toBeInTheDocument();
    expect(screen.getByText('cache-v2')).toBeInTheDocument();
  });

  it('should display Smart-Reload introduction alert', () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    render(<ReloadListPage />);

    // Verify introduction alert is displayed
    expect(screen.getByText(/Smart-Reload 心跳服務介紹/)).toBeInTheDocument();
    expect(screen.getByText(/簡介：SmartReload是一個可以在不重啟進程的情況下動態重新加載配置/)).toBeInTheDocument();
  });

  it('should display execute button when user has execute privilege', async () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockReloadData,
    });

    render(<ReloadListPage />);

    await waitFor(() => {
      expect(screen.getByText('ConfigReload')).toBeInTheDocument();
    });

    // Verify execute buttons are displayed (2 records = 2 execute buttons)
    const executeButtons = screen.getAllByRole('button', { name: /執行/ });
    expect(executeButtons.length).toBe(2);
  });

  it('should display view result button when user has result privilege', async () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockReloadData,
    });

    render(<ReloadListPage />);

    await waitFor(() => {
      expect(screen.getByText('ConfigReload')).toBeInTheDocument();
    });

    // Verify view result buttons are displayed (2 records = 2 buttons)
    const resultButtons = screen.getAllByRole('button', { name: /查看結果/ });
    expect(resultButtons.length).toBe(2);
  });

  it('should open DoReloadFormModal when execute button is clicked', async () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockReloadData,
    });

    render(<ReloadListPage />);

    await waitFor(() => {
      expect(screen.getByText('ConfigReload')).toBeInTheDocument();
    });

    // Click the first execute button
    const executeButtons = screen.getAllByRole('button', { name: /執行/ });
    fireEvent.click(executeButtons[0]);

    // Verify DoReloadFormModal is opened
    await waitFor(() => {
      expect(screen.getByText('執行Reload')).toBeInTheDocument();
    });
  });

  it('should open ReloadResultModal when view result button is clicked', async () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockReloadData,
    });

    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    render(<ReloadListPage />);

    await waitFor(() => {
      expect(screen.getByText('ConfigReload')).toBeInTheDocument();
    });

    // Click the first view result button
    const resultButtons = screen.getAllByRole('button', { name: /查看結果/ });
    fireEvent.click(resultButtons[0]);

    // Verify ReloadResultModal is opened
    await waitFor(() => {
      expect(screen.getByText('reload結果列表')).toBeInTheDocument();
    });
  });

  it('should handle empty data gracefully', async () => {
    vi.mocked(reloadApi.queryList).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    render(<ReloadListPage />);

    // Verify table is rendered with no data
    await waitFor(() => {
      expect(screen.getByText('標籤')).toBeInTheDocument();
    });

    // Verify table is rendered (at least one table exists)
    const tables = screen.getAllByRole('table');
    expect(tables.length).toBeGreaterThan(0);
  });

  it('should handle API error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    vi.mocked(reloadApi.queryList).mockRejectedValue(new Error('Network error'));

    render(<ReloadListPage />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Failed to fetch reload list:',
        expect.any(Error)
      );
    });

    consoleErrorSpy.mockRestore();
  });
});
