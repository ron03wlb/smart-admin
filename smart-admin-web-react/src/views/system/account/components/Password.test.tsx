/**
 * Password Component Unit Tests
 * 修改密碼組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Password from './Password';

// Mock dependencies
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    getPasswordComplexityEnabled: vi.fn(),
    updateEmployeePassword: vi.fn(),
  },
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
    },
  };
});

import { employeeApi } from '@/api/system/employeeApi';
import { message } from 'antd';

describe('Password', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock getPasswordComplexityEnabled - default to false
    (employeeApi.getPasswordComplexityEnabled as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: false,
    });

    // Mock updateEmployeePassword
    (employeeApi.updateEmployeePassword as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });
  });

  // ==================== 基本渲染測試 ====================

  describe('Basic Rendering', () => {
    it('should render all form fields', async () => {
      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
        expect(screen.getByLabelText('新密碼')).toBeInTheDocument();
        expect(screen.getByLabelText('確認密碼')).toBeInTheDocument();
      });
    });

    it('should render submit button', () => {
      render(<Password />);

      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      expect(submitButton).toBeInTheDocument();
    });

    it('should display basic password tips when complexity is disabled', async () => {
      render(<Password />);

      await waitFor(() => {
        const tips = screen.getAllByText('密碼長度至少8位');
        expect(tips.length).toBeGreaterThan(0);
      });
    });

    it('should display complex password tips when complexity is enabled', async () => {
      (employeeApi.getPasswordComplexityEnabled as any).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: true,
      });

      render(<Password />);

      await waitFor(() => {
        const tips = screen.getAllByText(/密碼長度8-20位，必須包含字母、數字、特殊符號/i);
        expect(tips.length).toBeGreaterThan(0);
      });
    });
  });

  // ==================== 表單驗證測試 ====================

  describe('Form Validation', () => {
    it('should validate required fields', async () => {
      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Submit without filling any fields
      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      // Ant Design Form shows validation errors
      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('請檢查表單必填項');
      });

      expect(employeeApi.updateEmployeePassword).not.toHaveBeenCalled();
    });

    it('should validate minimum password length (basic mode)', async () => {
      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill form with short password
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'oldpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'short' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'short' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        const errors = screen.getAllByText('密碼長度至少8位');
        expect(errors.length).toBeGreaterThan(0);
      });

      expect(employeeApi.updateEmployeePassword).not.toHaveBeenCalled();
    });

    it('should validate password complexity when enabled', async () => {
      (employeeApi.getPasswordComplexityEnabled as any).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: true,
      });

      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill form with non-complex password (only numbers)
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'OldPassword@123' } });
      fireEvent.change(newPasswordInput, { target: { value: '12345678' } }); // Only numbers, 8 chars
      fireEvent.change(confirmPasswordInput, { target: { value: '12345678' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      // The regex validation will fail and show 密碼格式錯誤
      await waitFor(() => {
        const errors = screen.getAllByText('密碼格式錯誤');
        expect(errors.length).toBeGreaterThan(0);
      });

      expect(employeeApi.updateEmployeePassword).not.toHaveBeenCalled();
    });

    it('should check password confirmation mismatch', async () => {
      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill form with mismatched passwords
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'oldpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpass123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'differentpass' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('新密碼與確認密碼不一致');
      });

      expect(employeeApi.updateEmployeePassword).not.toHaveBeenCalled();
    });
  });

  // ==================== 表單提交測試 ====================

  describe('Form Submission', () => {
    it('should submit password change successfully', async () => {
      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill form with valid data
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'oldpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpass123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpass123' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(employeeApi.updateEmployeePassword).toHaveBeenCalledWith({
          oldPassword: 'oldpass123',
          newPassword: 'newpass123',
        });
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('修改成功');
      });
    });

    it('should reset form after successful submission', async () => {
      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill and submit form
      const oldPasswordInput = screen.getByLabelText('原密碼') as HTMLInputElement;
      const newPasswordInput = screen.getByLabelText('新密碼') as HTMLInputElement;
      const confirmPasswordInput = screen.getByLabelText('確認密碼') as HTMLInputElement;

      fireEvent.change(oldPasswordInput, { target: { value: 'oldpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpass123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpass123' } });

      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('修改成功');
      });

      // Wait for form reset to complete
      await waitFor(() => {
        expect(oldPasswordInput.value).toBe('');
      }, { timeout: 2000 });
    });

    it('should submit with complex password when complexity is enabled', async () => {
      (employeeApi.getPasswordComplexityEnabled as any).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: true,
      });

      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill form with complex password
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'OldPass@123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'NewPass@456' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'NewPass@456' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(employeeApi.updateEmployeePassword).toHaveBeenCalledWith({
          oldPassword: 'OldPass@123',
          newPassword: 'NewPass@456',
        });
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('修改成功');
      });
    });
  });

  // ==================== 錯誤處理測試 ====================

  describe('Error Handling', () => {
    it('should handle API error when loading complexity config', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      (employeeApi.getPasswordComplexityEnabled as any).mockRejectedValue(new Error('Network error'));

      render(<Password />);

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith('獲取密碼複雜度配置失敗:', expect.any(Error));
      });

      consoleErrorSpy.mockRestore();
    });

    it('should handle API error when submitting', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      (employeeApi.updateEmployeePassword as any).mockRejectedValue(new Error('Update error'));

      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill and submit form
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'oldpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpass123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpass123' } });

      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith('修改密碼失敗:', expect.any(Error));
      });

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('修改失敗');
      });

      consoleErrorSpy.mockRestore();
    });
  });

  // ==================== Loading 狀態測試 ====================

  describe('Loading State', () => {
    it('should show loading state while submitting', async () => {
      // Mock a delayed API call
      let resolvePromise: any;
      const delayedPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      (employeeApi.updateEmployeePassword as any).mockReturnValue(delayedPromise);

      render(<Password />);

      await waitFor(() => {
        expect(screen.getByLabelText('原密碼')).toBeInTheDocument();
      });

      // Fill and submit form
      const oldPasswordInput = screen.getByLabelText('原密碼');
      const newPasswordInput = screen.getByLabelText('新密碼');
      const confirmPasswordInput = screen.getByLabelText('確認密碼');

      fireEvent.change(oldPasswordInput, { target: { value: 'oldpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpass123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpass123' } });

      const submitButton = screen.getByRole('button', { name: /修改密碼/i });
      fireEvent.click(submitButton);

      // Check loading state immediately after click
      await waitFor(() => {
        const loadingButton = screen.getByRole('button', { name: /修改密碼/i }) as HTMLButtonElement;
        expect(loadingButton.className).toContain('ant-btn-loading');
      });

      // Resolve the promise to clean up
      resolvePromise({ code: 200, ok: true, msg: 'Success', data: undefined });
    });
  });
});
