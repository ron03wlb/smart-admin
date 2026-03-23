/**
 * JobFormModal Component Unit Tests
 * 定時任務表單 Modal 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import JobFormModal from './JobFormModal';
import { jobApi } from '@/api/support/jobApi';
import type { JobVO } from '../types';
import { message } from 'antd';

// Mock jobApi
vi.mock('@/api/support/jobApi', () => ({
  jobApi: {
    addJob: vi.fn(),
    updateJob: vi.fn(),
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

describe('JobFormModal', () => {
  const mockOnSuccess = vi.fn();
  const ref = { current: null } as any;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // P0 測試 1: Modal 渲染測試（新增模式）
  it('should render modal in add mode when show is called without data', () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    // 驗證 Modal 標題為「添加」
    expect(screen.getByText('添加')).toBeInTheDocument();

    // 驗證表單字段存在
    expect(screen.getByText('任務名稱')).toBeInTheDocument();
    expect(screen.getByText('任務描述')).toBeInTheDocument();
    expect(screen.getByText('排序')).toBeInTheDocument();
    expect(screen.getByText('執行類')).toBeInTheDocument();
    expect(screen.getByText('任務參數')).toBeInTheDocument();
    expect(screen.getByText('觸發類型')).toBeInTheDocument();
    expect(screen.getByText('觸發時間')).toBeInTheDocument();
    expect(screen.getByText('是否開啟')).toBeInTheDocument();
  });

  // P0 測試 2: Modal 渲染測試（編輯模式）
  it('should render modal in edit mode when show is called with data', () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    // 驗證 Modal 標題為「編輯」
    expect(screen.getByText('編輯')).toBeInTheDocument();
  });

  // P0 測試 3: 表單字段預填測試（編輯模式）
  it('should pre-fill form fields when editing', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByDisplayValue('測試任務')).toBeInTheDocument();
    });

    // 驗證執行類回填
    expect(screen.getByDisplayValue('net.lab1024.sa.SmartJobSample1')).toBeInTheDocument();

    // 驗證 Cron 表達式回填
    expect(screen.getByDisplayValue('0 0/1 * * * ?')).toBeInTheDocument();

    // 驗證任務參數回填
    expect(screen.getByDisplayValue('{"key": "value"}')).toBeInTheDocument();

    // 驗證備註回填
    expect(screen.getByDisplayValue('測試備註')).toBeInTheDocument();

    // 驗證排序回填（使用 InputNumber 的 spinbutton role）
    const sortInput = screen.getByRole('spinbutton', { name: /排序/ });
    expect(sortInput).toHaveValue(1);
  });

  // P0 測試 4: 觸發類型切換測試（CRON ⇄ FIXED_DELAY）
  it('should switch trigger value input when trigger type is changed', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(
      () => {
        expect(screen.getByText('添加')).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 初始狀態：CRON 觸發類型，顯示文本輸入框
    const cronInput = screen.getByPlaceholderText('示例：10 15 0/1 * * *');
    expect(cronInput).toBeInTheDocument();

    // 切換為 FIXED_DELAY
    const fixedDelayButton = screen.getByText('固定間隔');
    fireEvent.click(fixedDelayButton);

    // 驗證輸入框切換為 InputNumber
    await waitFor(
      () => {
        const delayInput = screen.getByPlaceholderText('秒');
        expect(delayInput).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 切回 CRON
    const cronButton = screen.getByText('Cron表達式');
    fireEvent.click(cronButton);

    // 驗證輸入框切回文本框
    await waitFor(
      () => {
        const cronInputAgain = screen.getByPlaceholderText('示例：10 15 0/1 * * *');
        expect(cronInputAgain).toBeInTheDocument();
      },
      { timeout: 10000 }
    );
  }, 10000);

  // P0 測試 5: Cron 表達式輸入測試
  it('should accept cron expression input', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(() => {
      expect(screen.getByText('添加')).toBeInTheDocument();
    });

    // 找到 Cron 輸入框
    const cronInput = screen.getByPlaceholderText('示例：10 15 0/1 * * *');

    // 輸入 Cron 表達式
    fireEvent.change(cronInput, { target: { value: '0 0/5 * * * ?' } });

    // 驗證輸入值
    expect(cronInput).toHaveValue('0 0/5 * * * ?');
  });

  // P0 測試 6: 固定延遲輸入測試
  it('should accept fixed delay input', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(
      () => {
        expect(screen.getByText('添加')).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 切換為 FIXED_DELAY
    const fixedDelayButton = screen.getByText('固定間隔');
    fireEvent.click(fixedDelayButton);

    // 找到 InputNumber 輸入框
    await waitFor(
      () => {
        const delayInput = screen.getByPlaceholderText('秒');
        expect(delayInput).toBeInTheDocument();

        // 輸入延遲秒數
        fireEvent.change(delayInput, { target: { value: '60' } });

        // 驗證輸入值
        expect(delayInput).toHaveValue('60');
      },
      { timeout: 10000 }
    );
  }, 10000);

  // P0 測試 7: 表單驗證測試（必填字段）
  it('should validate required fields', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(
      () => {
        expect(screen.getByText('添加')).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 不填任何字段，直接點擊確認
    await waitFor(
      () => {
        const buttons = screen.getAllByRole('button');
        const okButton = buttons.find(btn => btn.textContent?.includes('確認'));
        expect(okButton).toBeDefined();
        fireEvent.click(okButton!);
      },
      { timeout: 10000 }
    );

    // 驗證必填字段錯誤提示（任務名稱、排序、執行類、觸發類型）
    await waitFor(
      () => {
        expect(screen.getByText('請輸入任務名稱')).toBeInTheDocument();
        expect(screen.getByText('請輸入排序')).toBeInTheDocument();
        expect(screen.getByText('請輸入執行類')).toBeInTheDocument();
        // 觸發類型有默認值 CRON，不會報錯
      },
      { timeout: 10000 }
    );

    // 驗證 API 不被調用
    expect(jobApi.addJob).not.toHaveBeenCalled();
  }, 10000);

  // P0 測試 8: 表單驗證測試（觸發時間必填）
  it('should validate trigger value is required', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(
      () => {
        expect(screen.getByText('添加')).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 填寫必填字段（除了觸發時間）
    const nameInput = screen.getByPlaceholderText('請輸入任務名稱');
    fireEvent.change(nameInput, { target: { value: '測試任務' } });

    const classInput = screen.getByPlaceholderText(
      '示例：net.lab1024.sa.base.module.support.job.sample.SmartJobSample1'
    );
    fireEvent.change(classInput, {
      target: { value: 'net.lab1024.sa.SmartJobSample1' },
    });

    const sortInput = screen.getByPlaceholderText('值越小越靠前');
    fireEvent.change(sortInput, { target: { value: '1' } });

    // 不填觸發時間，點擊確認
    await waitFor(
      () => {
        const buttons = screen.getAllByRole('button');
        const okButton = buttons.find(btn => btn.textContent?.includes('確認'));
        expect(okButton).toBeDefined();
        fireEvent.click(okButton!);
      },
      { timeout: 10000 }
    );

    // 驗證自定義錯誤提示
    await waitFor(
      () => {
        expect(message.error).toHaveBeenCalledWith('請填寫觸發時間');
      },
      { timeout: 10000 }
    );

    // 驗證 API 不被調用
    expect(jobApi.addJob).not.toHaveBeenCalled();
  }, 10000);

  // P0 測試 9: 新增任務成功測試
  it('should add job successfully', async () => {
    vi.mocked(jobApi.addJob).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'success',
      data: 'success',
    });

    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(
      () => {
        expect(screen.getByText('添加')).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 填寫表單
    const nameInput = screen.getByPlaceholderText('請輸入任務名稱');
    fireEvent.change(nameInput, { target: { value: '新任務' } });

    const classInput = screen.getByPlaceholderText(
      '示例：net.lab1024.sa.base.module.support.job.sample.SmartJobSample1'
    );
    fireEvent.change(classInput, {
      target: { value: 'net.lab1024.sa.SmartJobSample1' },
    });

    const sortInput = screen.getByPlaceholderText('值越小越靠前');
    fireEvent.change(sortInput, { target: { value: '1' } });

    const cronInput = screen.getByPlaceholderText('示例：10 15 0/1 * * *');
    fireEvent.change(cronInput, { target: { value: '0 0/1 * * * ?' } });

    // 點擊確認
    await waitFor(
      () => {
        const buttons = screen.getAllByRole('button');
        const okButton = buttons.find(btn => btn.textContent?.includes('確認'));
        expect(okButton).toBeDefined();
        fireEvent.click(okButton!);
      },
      { timeout: 10000 }
    );

    // 驗證 API 被調用
    await waitFor(
      () => {
        expect(jobApi.addJob).toHaveBeenCalledWith(
          expect.objectContaining({
            jobName: '新任務',
            jobClass: 'net.lab1024.sa.SmartJobSample1',
            triggerType: 'CRON',
            triggerValue: '0 0/1 * * * ?',
            sort: 1,
          })
        );
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalledTimes(1);
      },
      { timeout: 10000 }
    );
  }, 10000);

  // P0 測試 10: 編輯任務成功測試
  it('should update job successfully', async () => {
    vi.mocked(jobApi.updateJob).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'success',
      data: 'success',
    });

    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(
      () => {
        expect(screen.getByText('編輯')).toBeInTheDocument();
      },
      { timeout: 10000 }
    );

    // 修改任務名稱
    const nameInput = screen.getByDisplayValue('測試任務');
    fireEvent.change(nameInput, { target: { value: '修改後的任務' } });

    // 點擊確認
    await waitFor(
      () => {
        const buttons = screen.getAllByRole('button');
        const okButton = buttons.find(btn => btn.textContent?.includes('確認'));
        expect(okButton).toBeDefined();
        fireEvent.click(okButton!);
      },
      { timeout: 10000 }
    );

    // 驗證 API 被調用
    await waitFor(
      () => {
        expect(jobApi.updateJob).toHaveBeenCalledWith(
          expect.objectContaining({
            jobId: 1,
            jobName: '修改後的任務',
            jobClass: 'net.lab1024.sa.SmartJobSample1',
            triggerType: 'CRON',
            triggerValue: '0 0/1 * * * ?',
          })
        );
        expect(message.success).toHaveBeenCalledWith('更新成功');
        expect(mockOnSuccess).toHaveBeenCalledTimes(1);
      },
      { timeout: 10000 }
    );
  }, 10000);

  // P1 測試 11 (skip): API 錯誤處理測試（新增）
  it.skip('should handle add job API error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    vi.mocked(jobApi.addJob).mockRejectedValue(new Error('Network error'));

    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(() => {
      expect(screen.getByText('添加')).toBeInTheDocument();
    });

    // 填寫表單並提交
    const nameInput = screen.getByPlaceholderText('請輸入任務名稱');
    fireEvent.change(nameInput, { target: { value: '新任務' } });

    const classInput = screen.getByPlaceholderText(
      '示例：net.lab1024.sa.base.module.support.job.sample.SmartJobSample1'
    );
    fireEvent.change(classInput, {
      target: { value: 'net.lab1024.sa.SmartJobSample1' },
    });

    const sortInput = screen.getByPlaceholderText('值越小越靠前');
    fireEvent.change(sortInput, { target: { value: '1' } });

    const cronInput = screen.getByPlaceholderText('示例：10 15 0/1 * * *');
    fireEvent.change(cronInput, { target: { value: '0 0/1 * * * ?' } });

    await waitFor(
      () => {
        const buttons = screen.getAllByRole('button');
        const okButton = buttons.find(btn => btn.textContent?.includes('確認'));
        expect(okButton).toBeDefined();
        fireEvent.click(okButton!);
      },
      { timeout: 10000 }
    );

    // 驗證錯誤處理
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('表單驗證失敗:', expect.any(Error));
    });

    consoleErrorSpy.mockRestore();
  });

  // P1 測試 12 (skip): API 錯誤處理測試（編輯）
  it.skip('should handle update job API error gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    vi.mocked(jobApi.updateJob).mockRejectedValue(new Error('Network error'));

    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show(mockJobData);
    });

    await waitFor(() => {
      expect(screen.getByText('編輯')).toBeInTheDocument();
    });

    // 修改並提交
    const nameInput = screen.getByDisplayValue('測試任務');
    fireEvent.change(nameInput, { target: { value: '修改後的任務' } });

    await waitFor(
      () => {
        const buttons = screen.getAllByRole('button');
        const okButton = buttons.find(btn => btn.textContent?.includes('確認'));
        expect(okButton).toBeDefined();
        fireEvent.click(okButton!);
      },
      { timeout: 10000 }
    );

    // 驗證錯誤處理
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('表單驗證失敗:', expect.any(Error));
    });

    consoleErrorSpy.mockRestore();
  });

  // P1 測試 13 (skip): 取消按鈕測試
  it.skip('should close modal and reset form when cancel button is clicked', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(() => {
      expect(screen.getByText('添加')).toBeInTheDocument();
    });

    // 填寫部分表單
    const nameInput = screen.getByPlaceholderText('請輸入任務名稱');
    fireEvent.change(nameInput, { target: { value: '測試任務' } });

    // 點擊取消按鈕
    const cancelButton = screen.getByText('取消');
    fireEvent.click(cancelButton);

    // 驗證 Modal 關閉（標題消失）
    await waitFor(() => {
      expect(screen.queryByText('添加')).not.toBeInTheDocument();
    });

    // 重新打開 Modal，驗證表單已重置
    act(() => {
      ref.current?.show();
    });

    await waitFor(() => {
      const nameInputAfter = screen.getByPlaceholderText('請輸入任務名稱');
      expect(nameInputAfter).toHaveValue('');
    });
  });

  // P1 測試 14 (skip): Switch 開關狀態測試
  it.skip('should toggle enabled switch', async () => {
    render(<JobFormModal ref={ref} onSuccess={mockOnSuccess} />);

    act(() => {
      ref.current?.show();
    });

    await waitFor(() => {
      expect(screen.getByText('添加')).toBeInTheDocument();
    });

    // 找到 Switch 開關
    const switchElement = screen.getByRole('switch');
    expect(switchElement).not.toBeChecked();

    // 點擊開關
    fireEvent.click(switchElement);

    // 驗證開關狀態
    await waitFor(() => {
      expect(switchElement).toBeChecked();
    });
  });
});
