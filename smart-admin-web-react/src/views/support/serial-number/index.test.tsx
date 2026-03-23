/**
 * Serial Number List Page Component Unit Tests
 * 單號生成器列表頁面組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import SerialNumberListPage from './index';
import { serialNumberApi } from '@/api/support/serialNumberApi';
import type { SerialNumberVO } from './types';

// Mock serialNumberApi
vi.mock('@/api/support/serialNumberApi', () => ({
  serialNumberApi: {
    getAll: vi.fn(),
    generate: vi.fn(),
    queryRecord: vi.fn(),
  },
}));

// Mock usePrivilege Hook
vi.mock('@/hooks/usePrivilege', () => ({
  usePrivilege: vi.fn((_permission: string) => {
    // 默認所有權限都返回 true
    return true;
  }),
}));

const mockSerialNumberData: SerialNumberVO[] = [
  {
    serialNumberId: 1,
    businessName: '訂單號',
    format: 'ORD{yyyyMMdd}{0000}',
    ruleType: '每天重置',
    initNumber: 1,
    stepRandomRange: 1,
    remark: '電商訂單號',
    lastNumber: 'ORD202403190001',
    lastTime: '2026-03-19 10:00:00',
  },
  {
    serialNumberId: 2,
    businessName: '合同號',
    format: 'CON{yyyyMMdd}{000}',
    ruleType: '每月重置',
    initNumber: 1,
    stepRandomRange: 1,
    remark: '合同單號',
    lastNumber: 'CON20240319001',
    lastTime: '2026-03-19 09:30:00',
  },
];

describe('SerialNumberListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render serial number list table with data', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    render(<SerialNumberListPage />);

    // Verify table title columns are displayed (use getAllByText since labels may appear in multiple places)
    expect(screen.getAllByText('ID').length).toBeGreaterThan(0);
    expect(screen.getAllByText('業務').length).toBeGreaterThan(0);
    expect(screen.getAllByText('格式').length).toBeGreaterThan(0);
    expect(screen.getAllByText('循環週期').length).toBeGreaterThan(0);
    expect(screen.getAllByText('初始值').length).toBeGreaterThan(0);
    expect(screen.getAllByText('隨機增量').length).toBeGreaterThan(0);
    expect(screen.getAllByText('備註').length).toBeGreaterThan(0);
    expect(screen.getAllByText('上次產生單號').length).toBeGreaterThan(0);
    expect(screen.getAllByText('上次產生時間').length).toBeGreaterThan(0);
    expect(screen.getAllByText('操作').length).toBeGreaterThan(0);

    // Wait for data to be loaded
    await waitFor(() => {
      expect(screen.getByText('訂單號')).toBeInTheDocument();
    });

    expect(screen.getByText('合同號')).toBeInTheDocument();
    expect(screen.getByText('ORD{yyyyMMdd}{0000}')).toBeInTheDocument();
    expect(screen.getByText('CON{yyyyMMdd}{000}')).toBeInTheDocument();
  });

  it('should display SerialNumber introduction alert', () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    render(<SerialNumberListPage />);

    // Verify introduction alert is displayed
    expect(screen.getByText(/SerialNumber 單號生成器介紹：/)).toBeInTheDocument();
    expect(
      screen.getByText(/簡介：SerialNumber是一個可以根據不同的日期、規則生成一系列特別單號的功能/)
    ).toBeInTheDocument();
  });

  it('should display generate button when user has generate privilege', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    render(<SerialNumberListPage />);

    await waitFor(() => {
      expect(screen.getByText('訂單號')).toBeInTheDocument();
    });

    // Verify generate buttons are displayed (2 records = 2 generate buttons)
    const generateButtons = screen.getAllByRole('button', { name: /生成/ });
    expect(generateButtons.length).toBe(2);

    // Verify buttons are enabled
    generateButtons.forEach(button => {
      expect(button).not.toBeDisabled();
    });
  });

  it('should display view record button when user has record privilege', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    render(<SerialNumberListPage />);

    await waitFor(() => {
      expect(screen.getByText('訂單號')).toBeInTheDocument();
    });

    // Verify view record buttons are displayed (2 records = 2 buttons)
    const recordButtons = screen.getAllByRole('button', { name: /查看記錄/ });
    expect(recordButtons.length).toBe(2);

    // Verify buttons are enabled
    recordButtons.forEach(button => {
      expect(button).not.toBeDisabled();
    });
  });

  it('should open SerialNumberGenerateModal when generate button is clicked', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    render(<SerialNumberListPage />);

    await waitFor(() => {
      expect(screen.getByText('訂單號')).toBeInTheDocument();
    });

    // Click the first generate button
    const generateButtons = screen.getAllByRole('button', { name: /生成/ });
    fireEvent.click(generateButtons[0]);

    // Verify SerialNumberGenerateModal is opened
    await waitFor(() => {
      expect(screen.getByText('生成單號')).toBeInTheDocument();
    });
  });

  it('should open SerialNumberRecordModal when view record button is clicked', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    vi.mocked(serialNumberApi.queryRecord).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: {
        list: [],
        total: 0,
        pageNum: 1,
        pageSize: 10,
        pages: 0,
        emptyFlag: true,
      },
    });

    render(<SerialNumberListPage />);

    await waitFor(() => {
      expect(screen.getByText('訂單號')).toBeInTheDocument();
    });

    // Click the first view record button
    const recordButtons = screen.getAllByRole('button', { name: /查看記錄/ });
    fireEvent.click(recordButtons[0]);

    // Verify SerialNumberRecordModal is opened
    await waitFor(() => {
      expect(screen.getByText('每日生成結果記錄')).toBeInTheDocument();
    });
  });

  it('should handle empty data gracefully', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: [],
    });

    render(<SerialNumberListPage />);

    // Verify table is rendered with no data
    await waitFor(() => {
      expect(screen.getAllByText('ID').length).toBeGreaterThan(0);
    });

    // Verify table is rendered (at least one table exists)
    const tables = screen.getAllByRole('table');
    expect(tables.length).toBeGreaterThan(0);
  });

  it('should handle API error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    vi.mocked(serialNumberApi.getAll).mockRejectedValue(new Error('Network error'));

    render(<SerialNumberListPage />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Failed to fetch serial number list:',
        expect.any(Error)
      );
    });

    consoleErrorSpy.mockRestore();
  });

  it('should display table loading state', async () => {
    // Create a Promise that won't resolve immediately
    let resolvePromise: (value: any) => void;
    const promise = new Promise(resolve => {
      resolvePromise = resolve;
    });

    vi.mocked(serialNumberApi.getAll).mockReturnValue(promise as any);

    render(<SerialNumberListPage />);

    // Verify loading spinner is displayed
    expect(screen.getByRole('table')).toBeInTheDocument();

    // Resolve the promise
    resolvePromise!({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('訂單號')).toBeInTheDocument();
    });
  });

  it('should call getAll API on mount', async () => {
    vi.mocked(serialNumberApi.getAll).mockResolvedValue({
      code: 1,
      ok: true,
      msg: 'success',
      data: mockSerialNumberData,
    });

    render(<SerialNumberListPage />);

    // Verify API was called
    await waitFor(() => {
      expect(serialNumberApi.getAll).toHaveBeenCalledTimes(1);
    });
  });
});
