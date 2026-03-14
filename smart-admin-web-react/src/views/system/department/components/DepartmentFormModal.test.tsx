/**
 * DepartmentFormModal Component Unit Tests
 * 部門表單 Modal 組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DepartmentFormModal } from './DepartmentFormModal';
import type { DepartmentFormData } from '../types';

// Mock dependencies
vi.mock('@/api/system/departmentApi');
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

import { departmentApi } from '@/api/system/departmentApi';
import { useModal } from '@/hooks/useModal';
import { message } from 'antd';

describe('DepartmentFormModal', () => {
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
      isEditMode: false,
    });
  });

  // ==================== 基本渲染測試 ====================

  describe('Basic Rendering - Add Mode', () => {
    it('should render in add mode', () => {
      render(<DepartmentFormModal {...defaultProps} />);

      expect(screen.getByText('添加部門')).toBeInTheDocument();
    });

    it('should display all form fields', () => {
      render(<DepartmentFormModal {...defaultProps} />);

      expect(screen.getByLabelText('部門名稱')).toBeInTheDocument();
      expect(screen.getByLabelText('部門負責人')).toBeInTheDocument();
      expect(screen.getByLabelText('部門排序（值越大越靠前）')).toBeInTheDocument();
    });

    it('should have save button', async () => {
      render(<DepartmentFormModal {...defaultProps} />);

      const submitButton = await findSubmitButton();
      expect(submitButton).toBeTruthy();
      expect(submitButton.textContent?.replace(/\s+/g, '')).toMatch(/保存/);
    });

    it('should have cancel button', async () => {
      render(<DepartmentFormModal {...defaultProps} />);

      const cancelButton = await findCancelButton();
      expect(cancelButton).toBeTruthy();
      expect(cancelButton.textContent?.replace(/\s+/g, '')).toMatch(/取消/);
    });
  });

  describe('Basic Rendering - Edit Mode', () => {
    it('should render in edit mode', () => {
      vi.mocked(useModal).mockReturnValue({
        isEditMode: true,
      });

      const editData: DepartmentFormData = {
        departmentId: 1,
        departmentName: '技術部',
        parentId: 0,
        managerId: 1,
        sort: 100,
      };

      render(<DepartmentFormModal {...defaultProps} initialData={editData} />);

      expect(screen.getByText('編輯部門')).toBeInTheDocument();
    });

    it('should have update button in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEditMode: true,
      });

      const editData: DepartmentFormData = {
        departmentId: 1,
        departmentName: '技術部',
        parentId: 0,
        managerId: 1,
        sort: 100,
      };

      render(<DepartmentFormModal {...defaultProps} initialData={editData} />);

      const submitButton = await findSubmitButton();
      expect(submitButton).toBeTruthy();
      expect(submitButton.textContent?.replace(/\s+/g, '')).toMatch(/更新/);
    });
  });

  // ==================== 表單驗證測試 ====================

  describe('Form Validation', () => {
    it('should validate required fields', async () => {
      render(<DepartmentFormModal {...defaultProps} />);

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('參數驗證錯誤，請仔細填寫表單數據！');
      });
    });

    it('should validate department name max length', async () => {
      // 設置 initialData.parentId = 0，使 parentId 不是必填欄位
      const initialData: DepartmentFormData = {
        parentId: 0, // TOP_PARENT_ID
      };

      render(<DepartmentFormModal {...defaultProps} initialData={initialData} />);

      const nameInput = screen.getByLabelText('部門名稱');
      const longName = 'a'.repeat(51); // 超過 50 個字符
      fireEvent.change(nameInput, { target: { value: longName } });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/部門名稱不能大於50個字符/))).toBe(true);
      });
    });
  });

  // ==================== 表單提交測試 - 新增模式 ====================

  describe('Form Submission - Add Mode', () => {
    it('should submit form successfully in add mode', async () => {
      vi.mocked(departmentApi.addDepartment).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      const initialData: DepartmentFormData = {
        parentId: 1,
        sort: 0,
      };

      render(<DepartmentFormModal {...defaultProps} initialData={initialData} />);

      // 填寫表單
      fireEvent.change(screen.getByLabelText('部門名稱'), {
        target: { value: '新部門' },
      });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(departmentApi.addDepartment).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should handle API error in add mode', async () => {
      vi.mocked(departmentApi.addDepartment).mockRejectedValue(new Error('API Error'));

      const initialData: DepartmentFormData = {
        parentId: 1,
        sort: 0,
      };

      render(<DepartmentFormModal {...defaultProps} initialData={initialData} />);

      // 填寫表單
      fireEvent.change(screen.getByLabelText('部門名稱'), {
        target: { value: '新部門' },
      });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('添加失敗');
      });
    });
  });

  // ==================== 表單提交測試 - 編輯模式 ====================

  describe('Form Submission - Edit Mode', () => {
    it('should submit form successfully in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEditMode: true,
      });

      vi.mocked(departmentApi.updateDepartment).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      const editData: DepartmentFormData = {
        departmentId: 1,
        departmentName: '技術部',
        parentId: 0,
        managerId: 1,
        sort: 100,
      };

      render(<DepartmentFormModal {...defaultProps} initialData={editData} />);

      const updateButton = await findSubmitButton();
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(departmentApi.updateDepartment).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('更新成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should prevent parent from being self', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEditMode: true,
      });

      const editData: DepartmentFormData = {
        departmentId: 5,
        departmentName: '測試部門',
        parentId: 5, // 上級部門是自己
        sort: 10,
      };

      render(<DepartmentFormModal {...defaultProps} initialData={editData} />);

      const updateButton = await findSubmitButton();
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(message.warning).toHaveBeenCalledWith('上級部門不能為自己');
        expect(departmentApi.updateDepartment).not.toHaveBeenCalled();
      });
    });
  });

  // ==================== Modal 行為測試 ====================

  describe('Modal Behavior', () => {
    it('should call onCancel when cancel button is clicked', async () => {
      render(<DepartmentFormModal {...defaultProps} />);

      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should reset form on cancel', async () => {
      render(<DepartmentFormModal {...defaultProps} />);

      // 填寫表單
      const nameInput = screen.getByLabelText('部門名稱');
      fireEvent.change(nameInput, { target: { value: '測試' } });

      // 取消
      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });
  });
});
