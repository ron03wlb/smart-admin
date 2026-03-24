/**
 * DictFormModal Component Unit Tests
 * 字典表單組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DictFormModal from './DictFormModal';
import type { DictVO } from '../types';

// Mock dependencies
vi.mock('@/api/support/dictApi', () => ({
  dictApi: {
    addDict: vi.fn(),
    updateDict: vi.fn(),
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

const mockDict: DictVO = {
  dictId: 1,
  dictCode: 'GENDER',
  dictName: '性別',
  remark: '性別字典',
  disabledFlag: 0,
};

describe('DictFormModal', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    (dictApi.addDict as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });

    (dictApi.updateDict as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });
  });

  describe('Basic Rendering', () => {
    it('should render add mode modal', () => {
      render(<DictFormModal visible={true} onClose={mockOnClose} onSuccess={mockOnSuccess} />);

      expect(screen.getByText('添加字典')).toBeInTheDocument();
      expect(screen.getByLabelText('字典編碼')).toBeInTheDocument();
      expect(screen.getByLabelText('字典名稱')).toBeInTheDocument();
      expect(screen.getByLabelText('備註')).toBeInTheDocument();
    });

    it('should render edit mode modal', () => {
      render(
        <DictFormModal
          visible={true}
          dict={mockDict}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      expect(screen.getByText('編輯字典')).toBeInTheDocument();
    });

    it('should not render when visible is false', () => {
      render(<DictFormModal visible={false} onClose={mockOnClose} onSuccess={mockOnSuccess} />);

      expect(screen.queryByText('添加字典')).not.toBeInTheDocument();
    });
  });

  describe('Form Submission - Add Mode', () => {
    it('should submit new dict successfully', async () => {
      render(<DictFormModal visible={true} onClose={mockOnClose} onSuccess={mockOnSuccess} />);

      // Fill form
      fireEvent.change(screen.getByLabelText('字典編碼'), { target: { value: 'STATUS' } });
      fireEvent.change(screen.getByLabelText('字典名稱'), { target: { value: '狀態' } });
      fireEvent.change(screen.getByLabelText('備註'), { target: { value: '狀態字典' } });

      // Submit
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(dictApi.addDict).toHaveBeenCalledWith(
          expect.objectContaining({
            dictCode: 'STATUS',
            dictName: '狀態',
            remark: '狀態字典',
          })
        );
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });

    it('should validate required fields', async () => {
      render(<DictFormModal visible={true} onClose={mockOnClose} onSuccess={mockOnSuccess} />);

      // Submit without filling
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('參數驗證錯誤，請仔細填寫表單數據!');
      });

      expect(dictApi.addDict).not.toHaveBeenCalled();
    });
  });

  describe('Form Submission - Edit Mode', () => {
    it('should submit updated dict successfully', async () => {
      render(
        <DictFormModal
          visible={true}
          dict={mockDict}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      // Update name
      const nameInput = screen.getByDisplayValue('性別');
      fireEvent.change(nameInput, { target: { value: '性別更新' } });

      // Submit
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(dictApi.updateDict).toHaveBeenCalledWith(
          expect.objectContaining({
            dictId: 1,
            dictCode: 'GENDER',
            dictName: '性別更新',
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
      render(<DictFormModal visible={true} onClose={mockOnClose} onSuccess={mockOnSuccess} />);

      const cancelButton = screen.getByRole('button', { name: /取.*消/ });
      fireEvent.click(cancelButton);

      expect(mockOnClose).toHaveBeenCalled();
    });
  });
});
