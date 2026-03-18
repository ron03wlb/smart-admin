/**
 * DictDataFormModal Component Unit Tests
 * 字典值表單組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DictDataFormModal from './DictDataFormModal';
import type { DictDataVO } from '../types';

// Mock dependencies
vi.mock('@/api/support/dictApi', () => ({
  dictApi: {
    addDictData: vi.fn(),
    updateDictData: vi.fn(),
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

import { dictApi } from '@/api/support/dictApi';
import { message } from 'antd';

const mockDictData: DictDataVO = {
  dictDataId: 1,
  dictId: 10,
  dictCode: 'GENDER',
  dataValue: '1',
  dataLabel: '男',
  sortOrder: 100,
  remark: '男性',
  disabledFlag: 0,
  enabled: true,
};

describe('DictDataFormModal', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    (dictApi.addDictData as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });

    (dictApi.updateDictData as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });
  });

  describe('Basic Rendering', () => {
    it('should render add mode modal', () => {
      render(
        <DictDataFormModal
          visible={true}
          dictId={10}
          dictCode="GENDER"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      expect(screen.getByText('添加字典值')).toBeInTheDocument();
      expect(screen.getByLabelText('字典項名稱')).toBeInTheDocument();
      expect(screen.getByLabelText('字典項值')).toBeInTheDocument();
      expect(screen.getByLabelText('排序')).toBeInTheDocument();
    });

    it('should render edit mode modal', () => {
      render(
        <DictDataFormModal
          visible={true}
          dictData={mockDictData}
          dictId={10}
          dictCode="GENDER"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      expect(screen.getByText('編輯字典值')).toBeInTheDocument();
      expect(screen.getByDisplayValue('男')).toBeInTheDocument();
    });
  });

  describe('Form Submission - Add Mode', () => {
    it('should submit new dict data successfully', async () => {
      render(
        <DictDataFormModal
          visible={true}
          dictId={10}
          dictCode="GENDER"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      // Fill form
      fireEvent.change(screen.getByLabelText('字典項名稱'), { target: { value: '女' } });
      fireEvent.change(screen.getByLabelText('字典項值'), { target: { value: '2' } });

      // Submit
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(dictApi.addDictData).toHaveBeenCalledWith(
          expect.objectContaining({
            dictId: 10,
            dictCode: 'GENDER',
            dataLabel: '女',
            dataValue: '2',
          })
        );
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should validate required fields', async () => {
      render(
        <DictDataFormModal
          visible={true}
          dictId={10}
          dictCode="GENDER"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      // Submit without filling
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('參數驗證錯誤，請仔細填寫表單數據!');
      });

      expect(dictApi.addDictData).not.toHaveBeenCalled();
    });
  });

  describe('Form Submission - Edit Mode', () => {
    it('should submit updated dict data successfully', async () => {
      render(
        <DictDataFormModal
          visible={true}
          dictData={mockDictData}
          dictId={10}
          dictCode="GENDER"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      // Update label
      const labelInput = screen.getByDisplayValue('男');
      fireEvent.change(labelInput, { target: { value: '男性更新' } });

      // Submit
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(dictApi.updateDictData).toHaveBeenCalledWith(
          expect.objectContaining({
            dictDataId: 1,
            dataLabel: '男性更新',
          })
        );
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('修改成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });
  });

  describe('Modal Close', () => {
    it('should call onClose when cancel button is clicked', () => {
      render(
        <DictDataFormModal
          visible={true}
          dictId={10}
          dictCode="GENDER"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const cancelButton = screen.getByRole('button', { name: /取.*消/ });
      fireEvent.click(cancelButton);

      expect(mockOnClose).toHaveBeenCalled();
    });
  });
});
