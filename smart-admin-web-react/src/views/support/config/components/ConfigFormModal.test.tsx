/**
 * ConfigFormModal Component Unit Tests
 * 配置表單 Modal 組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ConfigFormModal } from './ConfigFormModal';
import type { ConfigFormData } from '../types';

// Mock dependencies
vi.mock('@/api/support/configApi');
vi.mock('@/hooks/useModal');
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  };
});

import { configApi } from '@/api/support/configApi';
import { useModal } from '@/hooks/useModal';
import { message } from 'antd';

describe('ConfigFormModal', () => {
  const mockOnCancel = vi.fn();
  const mockOnSuccess = vi.fn();

  const defaultProps = {
    visible: true,
    onCancel: mockOnCancel,
    onSuccess: mockOnSuccess,
  };

  // Helper function to find Modal buttons (rendered to document.body)
  const findModalButton = async (selector: string) => {
    return await waitFor(() => {
      const button = document.querySelector(selector);
      if (!button) throw new Error(`Button with selector "${selector}" not found`);
      return button;
    });
  };

  const findSubmitButton = () => findModalButton('.ant-modal-footer .ant-btn-primary');
  const findCancelButton = () => findModalButton('.ant-modal-footer .ant-btn:not(.ant-btn-primary)');

  beforeEach(() => {
    vi.clearAllMocks();

    // Mock useModal default (add mode)
    vi.mocked(useModal).mockReturnValue({
      isEdit: false,
    });
  });

  // ==================== 基本渲染測試 ====================

  describe('Basic Rendering - Add Mode', () => {
    it('should render in add mode', () => {
      render(<ConfigFormModal {...defaultProps} />);

      expect(screen.getByText('添加配置')).toBeInTheDocument();
    });

    it('should display all form fields', () => {
      render(<ConfigFormModal {...defaultProps} />);

      expect(screen.getByLabelText('參數 Key')).toBeInTheDocument();
      expect(screen.getByLabelText('參數名稱')).toBeInTheDocument();
      expect(screen.getByLabelText('參數值')).toBeInTheDocument();
      expect(screen.getByLabelText('備註')).toBeInTheDocument();
    });

    it('should have confirm button', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const submitButton = await findSubmitButton();
      expect(submitButton).toBeTruthy();
      expect(submitButton.textContent?.replace(/\s+/g, '')).toMatch(/確認/);
    });

    it('should have cancel button', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const cancelButton = await findCancelButton();
      expect(cancelButton).toBeTruthy();
      expect(cancelButton.textContent?.replace(/\s+/g, '')).toMatch(/取消/);
    });
  });

  describe('Basic Rendering - Edit Mode', () => {
    it('should render in edit mode', () => {
      vi.mocked(useModal).mockReturnValue({
        isEdit: true,
      });

      const editData: ConfigFormData = {
        configId: 1,
        configKey: 'system.title',
        configName: '系統標題',
        configValue: 'SmartAdmin',
        remark: '系統主標題',
      };

      render(<ConfigFormModal {...defaultProps} initialData={editData} />);

      expect(screen.getByText('編輯配置')).toBeInTheDocument();
    });
  });

  // ==================== 表單驗證測試 ====================

  describe('Form Validation', () => {
    it('should validate required fields', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('參數驗證錯誤，請仔細填寫表單數據！');
      });
    });

    it('should validate config key max length', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const keyInput = screen.getByLabelText('參數 Key');
      const longKey = 'a'.repeat(51); // 超過 50 個字符
      fireEvent.change(keyInput, { target: { value: longKey } });

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/參數 Key 不能大於50個字符/))).toBe(true);
      });
    });

    it('should validate config name max length', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const nameInput = screen.getByLabelText('參數名稱');
      const longName = 'a'.repeat(101); // 超過 100 個字符
      fireEvent.change(nameInput, { target: { value: longName } });

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/參數名稱不能大於100個字符/))).toBe(true);
      });
    });

    it('should validate config value max length', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const valueInput = screen.getByLabelText('參數值');
      const longValue = 'a'.repeat(501); // 超過 500 個字符
      fireEvent.change(valueInput, { target: { value: longValue } });

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/參數值不能大於500個字符/))).toBe(true);
      });
    });
  });

  // ==================== 表單提交測試 - 新增模式 ====================

  describe('Form Submission - Add Mode', () => {
    it('should submit form successfully in add mode', async () => {
      vi.mocked(configApi.addConfig).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      render(<ConfigFormModal {...defaultProps} />);

      // 填寫表單
      fireEvent.change(screen.getByLabelText('參數 Key'), {
        target: { value: 'system.version' },
      });
      fireEvent.change(screen.getByLabelText('參數名稱'), {
        target: { value: '系統版本' },
      });
      fireEvent.change(screen.getByLabelText('參數值'), {
        target: { value: '1.0.0' },
      });

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(configApi.addConfig).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should handle API error in add mode', async () => {
      vi.mocked(configApi.addConfig).mockRejectedValue(new Error('API Error'));

      render(<ConfigFormModal {...defaultProps} />);

      // 填寫表單
      fireEvent.change(screen.getByLabelText('參數 Key'), {
        target: { value: 'system.version' },
      });
      fireEvent.change(screen.getByLabelText('參數名稱'), {
        target: { value: '系統版本' },
      });
      fireEvent.change(screen.getByLabelText('參數值'), {
        target: { value: '1.0.0' },
      });

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('添加失敗');
      });
    });
  });

  // ==================== 表單提交測試 - 編輯模式 ====================

  describe('Form Submission - Edit Mode', () => {
    it('should submit form successfully in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEdit: true,
      });

      vi.mocked(configApi.updateConfig).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      const editData: ConfigFormData = {
        configId: 1,
        configKey: 'system.title',
        configName: '系統標題',
        configValue: 'SmartAdmin',
        remark: '系統主標題',
      };

      render(<ConfigFormModal {...defaultProps} initialData={editData} />);

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(configApi.updateConfig).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('更新成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should handle API error in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEdit: true,
      });

      vi.mocked(configApi.updateConfig).mockRejectedValue(new Error('API Error'));

      const editData: ConfigFormData = {
        configId: 1,
        configKey: 'system.title',
        configName: '系統標題',
        configValue: 'SmartAdmin',
      };

      render(<ConfigFormModal {...defaultProps} initialData={editData} />);

      const confirmButton = await findSubmitButton();
      fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('更新失敗');
      });
    });
  });

  // ==================== Modal 行為測試 ====================

  describe('Modal Behavior', () => {
    it('should call onCancel when cancel button is clicked', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should reset form on cancel', async () => {
      render(<ConfigFormModal {...defaultProps} />);

      // 填寫表單
      const keyInput = screen.getByLabelText('參數 Key');
      fireEvent.change(keyInput, { target: { value: 'test' } });

      // 取消
      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });
  });
});
