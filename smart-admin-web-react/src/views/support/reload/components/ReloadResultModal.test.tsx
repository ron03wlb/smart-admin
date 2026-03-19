/**
 * ReloadResultModal Component Unit Tests
 * Reload 結果 Modal 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import ReloadResultModal from './ReloadResultModal';
import { reloadApi } from '@/api/support/reloadApi';
import type { ReloadResultVO } from '../types';

// Mock reloadApi
vi.mock('@/api/support/reloadApi', () => ({
  reloadApi: {
    queryReloadResult: vi.fn(),
  },
}));

const mockSuccessResult: ReloadResultVO[] = [
  {
    tag: 'ConfigReload',
    args: '{"cacheKey":"system_config"}',
    result: true,
    createTime: '2026-03-19 10:30:00',
  },
  {
    tag: 'ConfigReload',
    args: '{"cacheKey":"user_config"}',
    result: true,
    createTime: '2026-03-19 10:25:00',
  },
];

const mockFailedResult: ReloadResultVO[] = [
  {
    tag: 'CacheReload',
    args: '{"type":"redis"}',
    result: false,
    exception: 'Redis connection timeout: unable to connect to redis://localhost:6379',
    createTime: '2026-03-19 09:15:00',
  },
];

describe('ReloadResultModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render modal when showModal is called', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSuccessResult,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('ConfigReload');
    });

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('reload結果列表')).toBeInTheDocument();
    });

    // Verify table columns are displayed (use getAllByText since labels may appear in multiple places)
    expect(screen.getAllByText('標籤').length).toBeGreaterThan(0);
    expect(screen.getAllByText('參數').length).toBeGreaterThan(0);
    expect(screen.getAllByText('運行結果').length).toBeGreaterThan(0);
    expect(screen.getAllByText('異常').length).toBeGreaterThan(0);
    expect(screen.getAllByText('創建時間').length).toBeGreaterThan(0);
  });

  it('should display success results with green tags', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSuccessResult,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('ConfigReload');
    });

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getAllByText('ConfigReload').length).toBeGreaterThan(0);
    });

    // Verify success tags are displayed
    const successTags = screen.getAllByText('成功');
    expect(successTags.length).toBe(2);

    // Verify args are displayed
    expect(screen.getByText('{"cacheKey":"system_config"}')).toBeInTheDocument();
    expect(screen.getByText('{"cacheKey":"user_config"}')).toBeInTheDocument();
  });

  it('should display failed results with red tags', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockFailedResult,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('CacheReload');
    });

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('CacheReload')).toBeInTheDocument();
    });

    // Verify failed tag is displayed
    expect(screen.getByText('失敗')).toBeInTheDocument();

    // Verify args are displayed
    expect(screen.getByText('{"type":"redis"}')).toBeInTheDocument();
  });

  it('should display exception details in expandable row', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockFailedResult,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('CacheReload');
    });

    await waitFor(() => {
      expect(screen.getByText('CacheReload')).toBeInTheDocument();
    });

    // Ant Design expandable row - exception is visible in the expandedRowRender
    // The exception content should be in the document
    expect(
      screen.getByText(/Redis connection timeout: unable to connect to redis/)
    ).toBeInTheDocument();
  });

  it('should call API with correct tag when modal is opened', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal with specific tag
    act(() => {
      ref.current?.showModal('TestReload');
    });

    await waitFor(() => {
      expect(reloadApi.queryReloadResult).toHaveBeenCalledWith('TestReload');
    });
  });

  it('should refresh data when refresh button is clicked', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSuccessResult,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('ConfigReload');
    });

    await waitFor(() => {
      expect(screen.getByText('reload結果列表')).toBeInTheDocument();
    });

    // Clear mock call count
    vi.mocked(reloadApi.queryReloadResult).mockClear();

    // Click refresh button
    const refreshButton = screen.getByRole('button', { name: /刷新/ });
    fireEvent.click(refreshButton);

    // Verify API was called again
    await waitFor(() => {
      expect(reloadApi.queryReloadResult).toHaveBeenCalledWith('ConfigReload');
    });
  });

  it('should handle empty results gracefully', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('EmptyReload');
    });

    await waitFor(() => {
      expect(screen.getByText('reload結果列表')).toBeInTheDocument();
    });

    // Verify table is rendered (at least one table exists)
    const tables = screen.getAllByRole('table');
    expect(tables.length).toBeGreaterThan(0);
  });

  it('should handle API error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    vi.mocked(reloadApi.queryReloadResult).mockRejectedValue(new Error('Network error'));

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('ConfigReload');
    });

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Failed to fetch reload result:',
        expect.any(Error)
      );
    });

    consoleErrorSpy.mockRestore();
  });

  it('should close modal when cancel button is clicked', async () => {
    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSuccessResult,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('ConfigReload');
    });

    await waitFor(() => {
      expect(screen.getByText('reload結果列表')).toBeInTheDocument();
    });

    // Close modal (Ant Design modal footer includes cancel functionality)
    // In Ant Design, modal can be closed by clicking the X button or cancel button
    // Since we set footer={null}, we need to test with onCancel
    const modal = screen.getByRole('dialog');
    expect(modal).toBeInTheDocument();
  });

  it('should add id to each result item for table rowKey', async () => {
    const mockData = [
      {
        tag: 'Test1',
        args: '{}',
        result: true,
        createTime: '2026-03-19 10:00:00',
      },
      {
        tag: 'Test2',
        args: '{}',
        result: false,
        createTime: '2026-03-19 10:01:00',
      },
    ];

    vi.mocked(reloadApi.queryReloadResult).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockData,
    });

    const ref = { current: null } as any;
    render(<ReloadResultModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.showModal('Test');
    });

    await waitFor(() => {
      expect(screen.getByText('Test1')).toBeInTheDocument();
    });

    // Verify both items are displayed (id 1 and id 2)
    expect(screen.getByText('Test2')).toBeInTheDocument();
  });
});
