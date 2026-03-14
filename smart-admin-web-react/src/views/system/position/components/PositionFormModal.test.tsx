/**
 * PositionFormModal Component Unit Tests
 * 職位表單 Modal 組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PositionFormModal } from './PositionFormModal';
import type { PositionFormData } from '../types';

// Mock dependencies
vi.mock('@/api/system/positionApi');
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

import { positionApi } from '@/api/system/positionApi';
import { useModal } from '@/hooks/useModal';
import { message } from 'antd';

describe('PositionFormModal', () => {
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
      render(<PositionFormModal {...defaultProps} />);

      expect(screen.getByText('添加職位')).toBeInTheDocument();
    });

    it('should display all form fields', () => {
      render(<PositionFormModal {...defaultProps} />);

      expect(screen.getByLabelText('職位名稱')).toBeInTheDocument();
      expect(screen.getByLabelText('職級')).toBeInTheDocument();
      expect(screen.getByLabelText('排序（值越大越靠前）')).toBeInTheDocument();
      expect(screen.getByLabelText('備註')).toBeInTheDocument();
    });

    it('should have save button', async () => {
      render(<PositionFormModal {...defaultProps} />);

      const submitButton = await findSubmitButton();
      expect(submitButton).toBeTruthy();
      expect(submitButton.textContent?.replace(/\s+/g, '')).toMatch(/保存/);
    });

    it('should have cancel button', async () => {
      render(<PositionFormModal {...defaultProps} />);

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

      const editData: PositionFormData = {
        positionId: 1,
        positionName: '部門經理',
        positionLevel: 'M1',
        sort: 100,
        remark: '部門負責人',
      };

      render(<PositionFormModal {...defaultProps} initialData={editData} />);

      expect(screen.getByText('編輯職位')).toBeInTheDocument();
    });

    it('should have update button in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEditMode: true,
      });

      const editData: PositionFormData = {
        positionId: 1,
        positionName: '部門經理',
        positionLevel: 'M1',
        sort: 100,
      };

      render(<PositionFormModal {...defaultProps} initialData={editData} />);

      const submitButton = await findSubmitButton();
      expect(submitButton).toBeTruthy();
      expect(submitButton.textContent?.replace(/\s+/g, '')).toMatch(/更新/);
    });
  });

  // ==================== 表單驗證測試 ====================

  describe('Form Validation', () => {
    it('should validate required fields', async () => {
      render(<PositionFormModal {...defaultProps} />);

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('參數驗證錯誤，請仔細填寫表單數據！');
      });
    });

    it('should validate position name max length', async () => {
      render(<PositionFormModal {...defaultProps} />);

      const nameInput = screen.getByLabelText('職位名稱');
      const longName = 'a'.repeat(31); // 超過 30 個字符
      fireEvent.change(nameInput, { target: { value: longName } });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/職位名稱不能大於30個字符/))).toBe(true);
      });
    });

    it('should validate position level max length', async () => {
      render(<PositionFormModal {...defaultProps} />);

      const nameInput = screen.getByLabelText('職位名稱');
      fireEvent.change(nameInput, { target: { value: '測試職位' } });

      const levelInput = screen.getByLabelText('職級');
      const longLevel = 'a'.repeat(31); // 超過 30 個字符
      fireEvent.change(levelInput, { target: { value: longLevel } });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/職級不能大於30個字符/))).toBe(true);
      });
    });

    it('should validate remark max length', async () => {
      render(<PositionFormModal {...defaultProps} />);

      const nameInput = screen.getByLabelText('職位名稱');
      fireEvent.change(nameInput, { target: { value: '測試職位' } });

      const remarkInput = screen.getByLabelText('備註');
      const longRemark = 'a'.repeat(201); // 超過 200 個字符
      fireEvent.change(remarkInput, { target: { value: longRemark } });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/備註不能大於200個字符/))).toBe(true);
      });
    });
  });

  // ==================== 表單提交測試 - 新增模式 ====================

  describe('Form Submission - Add Mode', () => {
    it('should submit form successfully in add mode', async () => {
      vi.mocked(positionApi.addPosition).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      render(<PositionFormModal {...defaultProps} />);

      // 填寫表單
      fireEvent.change(screen.getByLabelText('職位名稱'), {
        target: { value: '新職位' },
      });

      const saveButton = await findSubmitButton();
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(positionApi.addPosition).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should handle API error in add mode', async () => {
      vi.mocked(positionApi.addPosition).mockRejectedValue(new Error('API Error'));

      render(<PositionFormModal {...defaultProps} />);

      // 填寫表單
      fireEvent.change(screen.getByLabelText('職位名稱'), {
        target: { value: '新職位' },
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

      vi.mocked(positionApi.updatePosition).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      const editData: PositionFormData = {
        positionId: 1,
        positionName: '部門經理',
        positionLevel: 'M1',
        sort: 100,
        remark: '部門負責人',
      };

      render(<PositionFormModal {...defaultProps} initialData={editData} />);

      const updateButton = await findSubmitButton();
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(positionApi.updatePosition).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('更新成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should handle API error in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        isEditMode: true,
      });

      vi.mocked(positionApi.updatePosition).mockRejectedValue(new Error('API Error'));

      const editData: PositionFormData = {
        positionId: 1,
        positionName: '部門經理',
        positionLevel: 'M1',
        sort: 100,
      };

      render(<PositionFormModal {...defaultProps} initialData={editData} />);

      const updateButton = await findSubmitButton();
      fireEvent.click(updateButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('更新失敗');
      });
    });
  });

  // ==================== Modal 行為測試 ====================

  describe('Modal Behavior', () => {
    it('should call onCancel when cancel button is clicked', async () => {
      render(<PositionFormModal {...defaultProps} />);

      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should reset form on cancel', async () => {
      render(<PositionFormModal {...defaultProps} />);

      // 填寫表單
      const nameInput = screen.getByLabelText('職位名稱');
      fireEvent.change(nameInput, { target: { value: '測試' } });

      // 取消
      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });
  });
});
