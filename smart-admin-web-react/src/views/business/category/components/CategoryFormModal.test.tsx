/**
 * CategoryFormModal Component Unit Tests
 * 分類表單組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-18
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { createRef } from 'react';
import CategoryFormModal, { CategoryFormModalRef } from './CategoryFormModal';
import type { CategoryVO } from '../types';
import { CategoryTypeEnum } from '../types';

// Mock dependencies
vi.mock('@/api/business/categoryApi', () => ({
  categoryApi: {
    addCategory: vi.fn(),
    updateCategory: vi.fn(),
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

import { categoryApi } from '@/api/business/categoryApi';
import { message } from 'antd';

const mockCategory: CategoryVO = {
  categoryId: 1,
  categoryName: '電子產品',
  categoryType: CategoryTypeEnum.GOODS,
  parentId: undefined,
  sort: 1,
  remark: '電子產品分類',
  disabledFlag: false,
  createTime: '2026-03-18 10:00:00',
  updateTime: '2026-03-18 10:00:00',
};

describe('CategoryFormModal', () => {
  const mockOnReloadList = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    (categoryApi.addCategory as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });

    (categoryApi.updateCategory as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: undefined,
    });
  });

  describe('Basic Rendering', () => {
    it('should render add mode modal', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 自動等待
      expect(await screen.findByText('添加分類')).toBeInTheDocument();
      expect(await screen.findByLabelText('分類名稱')).toBeInTheDocument();
    });

    it('should render edit mode modal', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示編輯 Modal
      ref.current?.show(CategoryTypeEnum.GOODS, undefined, mockCategory);

      // 使用 findBy 自動等待
      expect(await screen.findByText('編輯分類')).toBeInTheDocument();
      expect(await screen.findByDisplayValue('電子產品')).toBeInTheDocument();
    });

    it('should render add child category modal', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示添加子分類 Modal
      ref.current?.show(CategoryTypeEnum.GOODS, 1);

      // 使用 findBy 自動等待
      expect(await screen.findByText('添加分類')).toBeInTheDocument();
    });
  });

  describe('Form Submission - Add Mode', () => {
    it('should submit new category successfully', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByLabelText('分類名稱');

      // 填寫表單
      fireEvent.change(nameInput, { target: { value: '數碼產品' } });

      // 提交
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(categoryApi.addCategory).toHaveBeenCalledWith({
          categoryName: '數碼產品',
          categoryType: CategoryTypeEnum.GOODS,
          parentId: undefined,
          sort: undefined,
          remark: undefined,
        });
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnReloadList).toHaveBeenCalledWith(undefined);
      });
    });

    it('should submit new child category successfully', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示添加子分類 Modal
      ref.current?.show(CategoryTypeEnum.GOODS, 1);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByLabelText('分類名稱');

      // 填寫表單
      fireEvent.change(nameInput, { target: { value: '手機' } });

      // 提交
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(categoryApi.addCategory).toHaveBeenCalledWith({
          categoryName: '手機',
          categoryType: CategoryTypeEnum.GOODS,
          parentId: 1,
          sort: undefined,
          remark: undefined,
        });
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnReloadList).toHaveBeenCalledWith(1);
      });
    });

    it('should validate required fields', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 等待 Modal 渲染
      await screen.findByLabelText('分類名稱');

      // 提交空表單
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(screen.getByText('請輸入分類名稱')).toBeInTheDocument();
      });

      expect(categoryApi.addCategory).not.toHaveBeenCalled();
    });

    it('should validate max length', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByLabelText('分類名稱');

      // 填寫超長名稱
      const longName = 'A'.repeat(31);
      fireEvent.change(nameInput, { target: { value: longName } });

      // 提交
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(screen.getByText(/最多30個字符/)).toBeInTheDocument();
      });

      expect(categoryApi.addCategory).not.toHaveBeenCalled();
    });
  });

  describe('Form Submission - Edit Mode', () => {
    it('should submit updated category successfully', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示編輯 Modal
      ref.current?.show(CategoryTypeEnum.GOODS, undefined, mockCategory);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByDisplayValue('電子產品');

      // 修改名稱
      fireEvent.change(nameInput, { target: { value: '電子產品更新' } });

      // 提交
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(categoryApi.updateCategory).toHaveBeenCalledWith({
          categoryId: 1,
          categoryName: '電子產品更新',
          categoryType: CategoryTypeEnum.GOODS,
          parentId: undefined,
          sort: 1,
          remark: '電子產品分類',
        });
      });

      await waitFor(() => {
        expect(message.success).toHaveBeenCalledWith('修改成功');
        expect(mockOnReloadList).toHaveBeenCalledWith(undefined);
      });
    });
  });

  describe('Modal Close', () => {
    it('should close modal when cancel button is clicked', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 等待 Modal 渲染
      expect(await screen.findByText('添加分類')).toBeInTheDocument();

      // 點擊取消
      const cancelButton = screen.getByRole('button', { name: /取.*消/ });
      fireEvent.click(cancelButton);

      await waitFor(() => {
        expect(screen.queryByText('添加分類')).not.toBeInTheDocument();
      });
    });

    it('should close modal after successful submission', async () => {
      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByLabelText('分類名稱');

      // 填寫並提交
      fireEvent.change(nameInput, { target: { value: '數碼產品' } });
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(screen.queryByText('添加分類')).not.toBeInTheDocument();
      });
    });
  });

  describe('API Error Handling', () => {
    it('should handle add category error', async () => {
      (categoryApi.addCategory as any).mockRejectedValueOnce(new Error('Network Error'));

      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示 Modal
      ref.current?.show(CategoryTypeEnum.GOODS);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByLabelText('分類名稱');

      // 填寫並提交
      fireEvent.change(nameInput, { target: { value: '數碼產品' } });
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('Network Error');
      });

      expect(mockOnReloadList).not.toHaveBeenCalled();
    });

    it('should handle update category error', async () => {
      (categoryApi.updateCategory as any).mockRejectedValueOnce(new Error('Update Failed'));

      const ref = createRef<CategoryFormModalRef>();
      render(<CategoryFormModal ref={ref} onReloadList={mockOnReloadList} />);

      // 顯示編輯 Modal
      ref.current?.show(CategoryTypeEnum.GOODS, undefined, mockCategory);

      // 使用 findBy 等待 Modal 渲染
      const nameInput = await screen.findByDisplayValue('電子產品');

      // 修改並提交
      fireEvent.change(nameInput, { target: { value: '電子產品更新' } });
      const okButton = screen.getByRole('button', { name: /確.*認/ });
      fireEvent.click(okButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('Update Failed');
      });

      expect(mockOnReloadList).not.toHaveBeenCalled();
    });
  });
});
