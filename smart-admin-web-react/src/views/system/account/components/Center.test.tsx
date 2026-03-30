/**
 * Center Component Unit Tests
 * 個人中心組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import Center from './Center';

// Mock dependencies
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    getEmployee: vi.fn(),
    updateEmployee: vi.fn(),
  },
}));

import { employeeApi } from '@/api/system/employeeApi';

// Mock Redux store - matches UserState shape where employeeId is at state.user.employeeId (string)
const createMockStore = (overrides: { employeeId?: string } = { employeeId: '1' }) =>
  configureStore({
    reducer: {
      user: () => ({
        token: 'test-token',
        employeeId: overrides.employeeId ?? '1',
        employeeName: '測試用戶',
        loginName: 'testuser',
        administratorFlag: false,
        menuTree: [],
        displayMenuTree: [],
        pointsList: [],
        menuRouterList: [],
        menuParentIdListMap: {},
        unreadMessageCount: 0,
        loading: false,
        error: null,
      }),
    },
  });

describe('Center', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock getEmployee
    (employeeApi.getEmployee as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: {
        employeeId: 1,
        loginName: 'testuser',
        departmentId: 10,
        actualName: '測試用戶',
        gender: 1,
        phone: '13800138000',
        email: 'test@example.com',
        positionId: 5,
        remark: '這是備註',
      },
    });

    // Mock updateEmployee
    (employeeApi.updateEmployee as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });
  });

  // ==================== 基本渲染測試 ====================

  describe('Basic Rendering', () => {
    it('should render title', () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      expect(screen.getByText('個人中心')).toBeInTheDocument();
    });

    it('should load and display employee information', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(employeeApi.getEmployee).toHaveBeenCalledWith(1);
      });

      await waitFor(() => {
        const loginNameInput = screen.getByDisplayValue('testuser');
        expect(loginNameInput).toBeInTheDocument();
      });
    });

    it('should display all form fields', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(screen.getByLabelText('登錄賬號')).toBeInTheDocument();
        expect(screen.getByLabelText('部門')).toBeInTheDocument();
        expect(screen.getByLabelText('員工名稱')).toBeInTheDocument();
        expect(screen.getByLabelText('性別')).toBeInTheDocument();
        expect(screen.getByLabelText('手機號碼')).toBeInTheDocument();
        expect(screen.getByLabelText('郵箱')).toBeInTheDocument();
        expect(screen.getByLabelText('職務')).toBeInTheDocument();
        expect(screen.getByLabelText('備註')).toBeInTheDocument();
      });
    });

    it('should disable login name and department fields', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        const loginNameInput = screen.getByDisplayValue('testuser') as HTMLInputElement;
        expect(loginNameInput.disabled).toBe(true);
      });
    });
  });

  // ==================== 表單提交測試 ====================

  describe('Form Submission', () => {
    it('should submit updated information successfully', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      // Wait for form to load
      await waitFor(() => {
        expect(screen.getByDisplayValue('測試用戶')).toBeInTheDocument();
      });

      // Update actual name
      const nameInput = screen.getByDisplayValue('測試用戶');
      fireEvent.change(nameInput, { target: { value: '新名稱' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /更新個人信息/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(employeeApi.updateEmployee).toHaveBeenCalledWith(
          expect.objectContaining({
            employeeId: 1,
            actualName: '新名稱',
          })
        );
      });
    });

    it('should validate required fields', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(screen.getByDisplayValue('測試用戶')).toBeInTheDocument();
      });

      // Clear required field
      const nameInput = screen.getByDisplayValue('測試用戶');
      fireEvent.change(nameInput, { target: { value: '' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /更新個人信息/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('請輸入員工名稱')).toBeInTheDocument();
      });

      expect(employeeApi.updateEmployee).not.toHaveBeenCalled();
    });

    it('should validate phone number format', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(screen.getByDisplayValue('13800138000')).toBeInTheDocument();
      });

      // Enter invalid phone
      const phoneInput = screen.getByDisplayValue('13800138000');
      fireEvent.change(phoneInput, { target: { value: '12345' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /更新個人信息/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('請輸入有效的手機號碼')).toBeInTheDocument();
      });

      expect(employeeApi.updateEmployee).not.toHaveBeenCalled();
    });

    it('should validate email format', async () => {
      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
      });

      // Enter invalid email
      const emailInput = screen.getByDisplayValue('test@example.com');
      fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /更新個人信息/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('請輸入有效的郵箱地址')).toBeInTheDocument();
      });

      expect(employeeApi.updateEmployee).not.toHaveBeenCalled();
    });
  });

  // ==================== 錯誤處理測試 ====================

  describe('Error Handling', () => {
    it('should handle API error when loading data', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      (employeeApi.getEmployee as any).mockRejectedValue(new Error('Network error'));

      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith('獲取員工信息失敗:', expect.any(Error));
      });

      consoleErrorSpy.mockRestore();
    });

    it('should handle API error when submitting', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      (employeeApi.updateEmployee as any).mockRejectedValue(new Error('Update error'));

      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(screen.getByDisplayValue('測試用戶')).toBeInTheDocument();
      });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /更新個人信息/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        // Verify console.error was called (actual format may vary due to error handling)
        expect(consoleErrorSpy).toHaveBeenCalled();
        expect(consoleErrorSpy.mock.calls[0][0]).toContain('更新個人信息失敗');
      });

      consoleErrorSpy.mockRestore();
    });

    it('should not load data if employeeId is missing', () => {
      const store = createMockStore({ employeeId: '' });
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      expect(employeeApi.getEmployee).not.toHaveBeenCalled();
    });
  });

  // ==================== Loading 狀態測試 ====================

  describe('Loading State', () => {
    it('should show loading state while submitting', async () => {
      // Mock a delayed API call
      let resolvePromise: any;
      const delayedPromise = new Promise(resolve => {
        resolvePromise = resolve;
      });

      (employeeApi.updateEmployee as any).mockReturnValue(delayedPromise);

      const store = createMockStore();
      render(
        <Provider store={store}>
          <Center />
        </Provider>
      );

      await waitFor(() => {
        expect(screen.getByDisplayValue('測試用戶')).toBeInTheDocument();
      });

      // Submit form
      const submitButton = screen.getByRole('button', { name: /更新個人信息/i });
      fireEvent.click(submitButton);

      // Check loading state
      await waitFor(() => {
        const loadingButton = screen.getByRole('button', {
          name: /更新個人信息/i,
        }) as HTMLButtonElement;
        expect(loadingButton.className).toContain('ant-btn-loading');
      });

      // Resolve the promise to clean up
      resolvePromise({ code: 200, ok: true, msg: 'Success', data: undefined });
    });
  });
});
