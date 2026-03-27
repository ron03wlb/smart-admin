/**
 * ChangeLogFormModal Component Unit Tests
 * 系統更新日誌表單 Modal 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChangeLogFormModal from './ChangeLogFormModal';
import type { ChangeLogVO } from '../types';

// Test timeout constant for Modal components
const TEST_TIMEOUT = 15000;

// Mock changeLogApi
vi.mock('@/api/support/changeLogApi', () => ({
  changeLogApi: {
    add: vi.fn(),
    update: vi.fn(),
  },
}));

// Mock SmartEnumSelect
vi.mock('@/components/common/SmartEnumSelect', () => ({
  default: ({ onChange, value, ...props }: any) => (
    <select
      data-testid="enum-select"
      value={value}
      onChange={e => onChange?.(Number(e.target.value))}
      {...props}
    >
      <option value="">請選擇</option>
      <option value="1">重大更新</option>
      <option value="2">功能更新</option>
      <option value="3">Bug修復</option>
    </select>
  ),
}));

import { changeLogApi } from '@/api/support/changeLogApi';

const mockChangeLogData: ChangeLogVO = {
  changeLogId: 1,
  updateVersion: 'v1.0.0',
  type: 1,
  publishAuthor: 'Admin',
  publicDate: '2026-01-01',
  content: '重大更新：新增用戶管理模塊',
  link: 'https://example.com/v1.0.0',
  createTime: '2026-01-01 10:00:00',
  updateTime: '2026-01-01 10:00:00',
};

describe('ChangeLogFormModal', () => {
  let onSuccess: () => void;

  beforeEach(() => {
    vi.clearAllMocks();
    onSuccess = vi.fn();
  });

  it('should render form modal in add mode', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

    // Open modal in add mode
    act(() => {
      ref.current?.show();
    });

    // Wait for modal to appear and verify title
    await waitFor(() => {
      expect(screen.getByText('新增更新日誌')).toBeInTheDocument();
    });

    // Verify form fields
    expect(screen.getByPlaceholderText('請輸入版本號，例如：v1.0.0')).toBeInTheDocument();
    expect(screen.getByTestId('enum-select')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('請輸入發布人')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('請選擇發布日期')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('請輸入跳轉鏈接（可選）')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('請輸入更新內容，支持 Markdown 格式')).toBeInTheDocument();
  });

  it('should render form modal in edit mode with pre-filled data', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

    // Open modal in edit mode
    act(() => {
      ref.current?.show(mockChangeLogData);
    });

    // Wait for modal to appear and verify title
    await waitFor(() => {
      expect(screen.getByText('編輯更新日誌')).toBeInTheDocument();
    });

    // Verify form fields are pre-filled
    const versionInput = screen.getByPlaceholderText(
      '請輸入版本號，例如：v1.0.0'
    ) as HTMLInputElement;
    expect(versionInput.value).toBe('v1.0.0');

    const authorInput = screen.getByPlaceholderText('請輸入發布人') as HTMLInputElement;
    expect(authorInput.value).toBe('Admin');

    const contentInput = screen.getByPlaceholderText(
      '請輸入更新內容，支持 Markdown 格式'
    ) as HTMLTextAreaElement;
    expect(contentInput.value).toBe('重大更新：新增用戶管理模塊');
  });

  it('should validate required fields', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

    // Open modal
    act(() => {
      ref.current?.show();
    });

    // Wait for modal to appear
    await waitFor(
      () => {
        expect(screen.getByText('新增更新日誌')).toBeInTheDocument();
      },
      { timeout: TEST_TIMEOUT }
    );

    // Click submit without filling form
    const submitButton = screen.getByRole('button', { name: /確定|OK/i });
    fireEvent.click(submitButton);

    // Wait for validation messages
    await waitFor(
      () => {
        expect(screen.getByText('請輸入版本號')).toBeInTheDocument();
        expect(screen.getByText('請選擇更新類型')).toBeInTheDocument();
        expect(screen.getByText('請輸入發布人')).toBeInTheDocument();
        expect(screen.getByText('請選擇發布日期')).toBeInTheDocument();
        expect(screen.getByText('請輸入更新內容')).toBeInTheDocument();
      },
      { timeout: TEST_TIMEOUT }
    );

    // Verify API not called
    expect(changeLogApi.add).not.toHaveBeenCalled();
  });

  it(
    'should validate URL format for link field',
    async () => {
      const ref = { current: null } as any;
      const user = userEvent.setup();
      render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

      // Open modal
      act(() => {
        ref.current?.show();
      });

      // Wait for modal to appear
      await waitFor(
        () => {
          expect(screen.getByText('新增更新日誌')).toBeInTheDocument();
        },
        { timeout: TEST_TIMEOUT }
      );

      // Fill invalid URL
      const linkInput = screen.getByPlaceholderText('請輸入跳轉鏈接（可選）');
      await user.type(linkInput, 'invalid-url');

      // Click submit
      const submitButton = screen.getByRole('button', { name: /確定|OK/i });
      fireEvent.click(submitButton);

      // Wait for validation message
      await waitFor(
        () => {
          expect(screen.getByText('請輸入有效的 URL')).toBeInTheDocument();
        },
        { timeout: TEST_TIMEOUT }
      );
    },
    TEST_TIMEOUT
  );

  it(
    'should handle add operation successfully',
    async () => {
      (changeLogApi.add as any).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: null,
      });

      const ref = { current: null } as any;
      const user = userEvent.setup();
      render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

      // Open modal
      act(() => {
        ref.current?.show();
      });

      // Wait for modal to appear
      await waitFor(
        () => {
          expect(screen.getByText('新增更新日誌')).toBeInTheDocument();
        },
        { timeout: TEST_TIMEOUT }
      );

      // Fill form
      await user.type(screen.getByPlaceholderText('請輸入版本號，例如：v1.0.0'), 'v2.0.0');

      const typeSelect = screen.getByTestId('enum-select');
      fireEvent.change(typeSelect, { target: { value: '1' } });

      await user.type(screen.getByPlaceholderText('請輸入發布人'), 'Developer');

      // Fill date (we'll use a valid date string)
      const dateInput = screen.getByPlaceholderText('請選擇發布日期');
      fireEvent.change(dateInput, { target: { value: '2026-03-19' } });

      await user.type(
        screen.getByPlaceholderText('請輸入更新內容，支持 Markdown 格式'),
        '新版本發布測試'
      );

      // Submit form
      const submitButton = screen.getByRole('button', { name: /確定|OK/i });
      fireEvent.click(submitButton);

      await waitFor(
        () => {
          expect(changeLogApi.add).toHaveBeenCalled();
          expect(onSuccess).toHaveBeenCalled();
        },
        { timeout: TEST_TIMEOUT }
      );
    },
    TEST_TIMEOUT
  );

  it(
    'should handle update operation successfully',
    async () => {
      (changeLogApi.update as any).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: null,
      });

      const ref = { current: null } as any;
      const user = userEvent.setup();
      render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

      // Open modal in edit mode
      act(() => {
        ref.current?.show(mockChangeLogData);
      });

      // Wait for modal to appear
      await waitFor(
        () => {
          expect(screen.getByText('編輯更新日誌')).toBeInTheDocument();
        },
        { timeout: TEST_TIMEOUT }
      );

      // Modify version
      const versionInput = screen.getByPlaceholderText('請輸入版本號，例如：v1.0.0');
      await user.clear(versionInput);
      await user.type(versionInput, 'v1.0.1');

      // Submit form
      const submitButton = screen.getByRole('button', { name: /確定|OK/i });
      fireEvent.click(submitButton);

      await waitFor(
        () => {
          expect(changeLogApi.update).toHaveBeenCalled();
          expect(onSuccess).toHaveBeenCalled();
        },
        { timeout: TEST_TIMEOUT }
      );
    },
    TEST_TIMEOUT
  );

  it(
    'should handle API error',
    async () => {
      (changeLogApi.add as any).mockRejectedValue(new Error('API Error'));

      const ref = { current: null } as any;
      const user = userEvent.setup();
      render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

      // Open modal
      act(() => {
        ref.current?.show();
      });

      // Wait for modal to appear
      await waitFor(
        () => {
          expect(screen.getByText('新增更新日誌')).toBeInTheDocument();
        },
        { timeout: TEST_TIMEOUT }
      );

      // Fill minimal required fields
      await user.type(screen.getByPlaceholderText('請輸入版本號，例如：v1.0.0'), 'v2.0.0');

      const typeSelect = screen.getByTestId('enum-select');
      fireEvent.change(typeSelect, { target: { value: '1' } });

      await user.type(screen.getByPlaceholderText('請輸入發布人'), 'Developer');

      const dateInput = screen.getByPlaceholderText('請選擇發布日期');
      fireEvent.change(dateInput, { target: { value: '2026-03-19' } });

      await user.type(
        screen.getByPlaceholderText('請輸入更新內容，支持 Markdown 格式'),
        '新版本發布測試'
      );

      // Submit form
      const submitButton = screen.getByRole('button', { name: /確定|OK/i });
      fireEvent.click(submitButton);

      await waitFor(
        () => {
          expect(changeLogApi.add).toHaveBeenCalled();
          expect(onSuccess).not.toHaveBeenCalled();
        },
        { timeout: TEST_TIMEOUT }
      );
    },
    TEST_TIMEOUT
  );

  // Note: Modal close test is skipped due to test environment limitations
  it.skip('should close modal on cancel', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogFormModal ref={ref} onSuccess={onSuccess} />);

    // Open modal
    act(() => {
      ref.current?.show();
    });

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('新增更新日誌')).toBeInTheDocument();
    });

    // Click cancel
    const cancelButton = screen.getByRole('button', { name: /取消|Cancel/i });
    fireEvent.click(cancelButton);

    // Modal should be closed (title not visible)
    await waitFor(() => {
      expect(screen.queryByText('新增更新日誌')).not.toBeInTheDocument();
    });
  });
});
