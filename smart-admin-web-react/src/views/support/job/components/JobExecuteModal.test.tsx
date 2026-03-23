/**
 * JobExecuteModal Component Unit Tests
 * 立即執行任務 Modal 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import JobExecuteModal from './JobExecuteModal';
import { jobApi } from '@/api/support/jobApi';
import type { JobVO } from '../types';
import { message } from 'antd';

// Mock jobApi
vi.mock('@/api/support/jobApi', () => ({
  jobApi: {
    executeJob: vi.fn(),
  },
}));

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  };
});

const mockJobData: JobVO = {
  jobId: 1,
  jobName: '測試任務',
  jobClass: 'net.lab1024.sa.SmartJobSample1',
  triggerType: 'CRON',
  triggerValue: '0 0/1 * * * ?',
  enabledFlag: true,
  param: '{"key": "value"}',
  remark: '測試備註',
  sort: 1,
  updateName: 'admin',
  updateTime: '2026-03-19 10:00:00',
};

describe('JobExecuteModal', () => {
  const mockOnSuccess = vi.fn();
  const ref = { current: null } as any;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // P0 測試 1: Modal 渲染測試
  it('should render modal when show is called', () => {
    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    // 驗證 Modal 標題為「執行任務」
    expect(screen.getByText('執行任務')).toBeInTheDocument();

    // 驗證表單字段存在
    expect(screen.getByText('任務名稱')).toBeInTheDocument();
    expect(screen.getByText('任務類名')).toBeInTheDocument();
    expect(screen.getByText('任務參數')).toBeInTheDocument();
  });

  // P0 測試 2: 任務信息回填測試（禁用編輯）
  it('should pre-fill job information and disable editing for name and class', async () => {
    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 驗證任務名稱回填並禁用
    const nameInput = screen.getByDisplayValue('測試任務');
    expect(nameInput).toBeInTheDocument();
    expect(nameInput).toBeDisabled();

    // 驗證任務類名回填並禁用
    const classInput = screen.getByDisplayValue('net.lab1024.sa.SmartJobSample1');
    expect(classInput).toBeInTheDocument();
    expect(classInput).toBeDisabled();

    // 驗證任務參數回填（但可編輯）
    const paramInput = screen.getByDisplayValue('{"key": "value"}');
    expect(paramInput).toBeInTheDocument();
    expect(paramInput).not.toBeDisabled();
  });

  // P0 測試 3: 參數可編輯測試
  it('should allow editing task parameters', async () => {
    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 找到任務參數輸入框
    const paramInput = screen.getByDisplayValue('{"key": "value"}');
    expect(paramInput).not.toBeDisabled();

    // 修改參數
    fireEvent.change(paramInput, { target: { value: '{"newKey": "newValue"}' } });

    // 驗證修改成功
    expect(paramInput).toHaveValue('{"newKey": "newValue"}');
  });

  // P0 測試 4: Alert 提示顯示測試
  it('should display execution alert message', async () => {
    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 驗證 Alert 提示信息存在
    const alertMessage = screen.getByText(
      /點擊【執行】後會按照【任務參數】，無論任務是否開啟，都會立即執行。/
    );
    expect(alertMessage).toBeInTheDocument();
  });

  // P0 測試 5: 執行成功測試（包含 2 秒延遲）
  it('should execute job successfully with 2-second delay', async () => {
    vi.mocked(jobApi.executeJob).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'success',
      data: '執行成功',
    });

    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 點擊執行按鈕
    const executeButton = screen.getByText('執行');
    fireEvent.click(executeButton);

    // 驗證 API 被調用
    await waitFor(() => {
      expect(jobApi.executeJob).toHaveBeenCalledWith({
        jobId: 1,
        updateName: undefined,
      });
    });

    // 等待 2 秒延遲後驗證成功提示
    await waitFor(
      () => {
        expect(message.success).toHaveBeenCalledWith('執行成功');
        expect(mockOnSuccess).toHaveBeenCalledTimes(1);
      },
      { timeout: 3000 }
    );
  }, 10000);

  // P0 測試 6: 取消按鈕測試
  it('should close modal when cancel button is clicked', async () => {
    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 點擊取消按鈕
    const cancelButton = screen.getByText('取消');
    fireEvent.click(cancelButton);

    // 驗證 Modal 關閉（標題消失）
    await waitFor(() => {
      expect(screen.queryByText('執行任務')).not.toBeInTheDocument();
    });

    // 驗證 onSuccess 不被調用
    expect(mockOnSuccess).not.toHaveBeenCalled();
  });

  // P1 測試 7 (skip): API 錯誤處理測試
  it.skip('should handle API error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    vi.mocked(jobApi.executeJob).mockRejectedValue(new Error('Network error'));

    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 點擊執行按鈕
    const executeButton = screen.getByText('執行');
    fireEvent.click(executeButton);

    // 驗證錯誤處理
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('執行任務失敗:', expect.any(Error));
    });

    consoleErrorSpy.mockRestore();
  });

  // P1 測試 8 (skip): 表單重置測試
  it.skip('should reset form when modal is closed', async () => {
    render(<JobExecuteModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('執行任務')).toBeInTheDocument();
    });

    // 修改參數
    const paramInput = screen.getByDisplayValue('{"key": "value"}');
    fireEvent.change(paramInput, { target: { value: '{"newKey": "newValue"}' } });

    // 點擊取消按鈕
    const cancelButton = screen.getByText('取消');
    fireEvent.click(cancelButton);

    // 重新打開 Modal，驗證表單已重置
    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      const paramInputAfter = screen.getByDisplayValue('{"key": "value"}');
      expect(paramInputAfter).toBeInTheDocument();
    });
  });
});
