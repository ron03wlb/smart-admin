/**
 * Category Management Page Tests
 * 分類管理頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-18
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { renderWithProviders, PERMISSIONS } from '@/test/test-utils';
import { Modal } from 'antd';
import CategoryManagement from './index';
import { categoryApi } from '@/api/business/categoryApi';
import type { CategoryVO } from './types';
import { CategoryTypeEnum } from './types';

// Mock categoryApi
vi.mock('@/api/business/categoryApi', () => ({
  categoryApi: {
    queryCategoryTree: vi.fn(),
    addCategory: vi.fn(),
    updateCategory: vi.fn(),
    deleteCategory: vi.fn(),
  },
}));

// Mock Ant Design message
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

import { message } from 'antd';

// Mock 數據
const mockCategoryTree: CategoryVO[] = [
  {
    categoryId: 1,
    categoryName: '電子產品',
    categoryType: CategoryTypeEnum.GOODS,
    parentId: undefined,
    sort: 1,
    remark: '電子產品分類',
    disabledFlag: false,
    createTime: '2026-03-18 10:00:00',
    updateTime: '2026-03-18 10:00:00',
    children: [
      {
        categoryId: 2,
        categoryName: '手機',
        categoryType: CategoryTypeEnum.GOODS,
        parentId: 1,
        sort: 1,
        remark: '手機分類',
        disabledFlag: false,
        createTime: '2026-03-18 10:05:00',
        updateTime: '2026-03-18 10:05:00',
      },
    ],
  },
  {
    categoryId: 3,
    categoryName: '服飾',
    categoryType: CategoryTypeEnum.GOODS,
    parentId: undefined,
    sort: 2,
    remark: '服飾分類',
    disabledFlag: false,
    createTime: '2026-03-18 10:10:00',
    updateTime: '2026-03-18 10:10:00',
  },
];

// Helper function to render with category permissions
const renderCategoryManagement = (administratorFlag = false) => {
  return renderWithProviders(<CategoryManagement />, {
    permissions: PERMISSIONS.ALL_CRUD('business:category').concat(['business:category:addChild']),
    administratorFlag,
  });
};

describe('CategoryManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock default response
    vi.mocked(categoryApi.queryCategoryTree).mockResolvedValue({
      code: 200,
      data: mockCategoryTree,
      ok: true,
      msg: '',
    });

    vi.mocked(categoryApi.deleteCategory).mockResolvedValue({
      code: 200,
      data: undefined,
      ok: true,
      msg: 'Success',
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Basic Rendering', () => {
    it('should render category management page', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('分類管理')).toBeInTheDocument();
      });

      expect(screen.getByText('新建分類')).toBeInTheDocument();
    });

    it('should call queryCategoryTree on mount', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalledWith({
          categoryType: CategoryTypeEnum.GOODS,
        });
      });
    });

    it('should display category tree data', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      expect(screen.getByText('服飾')).toBeInTheDocument();
    });
  });

  describe('Permission Control', () => {
    it('should show add button when user has permission', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('新建分類')).toBeInTheDocument();
      });
    });

    it('should show action buttons when user has permissions', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      // 應該有增加子分類、編輯、刪除按鈕
      const addChildButtons = screen.getAllByText('增加子分類');
      const editButtons = screen.getAllByText('編輯');
      const deleteButtons = screen.getAllByText('刪除');

      expect(addChildButtons.length).toBeGreaterThan(0);
      expect(editButtons.length).toBeGreaterThan(0);
      expect(deleteButtons.length).toBeGreaterThan(0);
    });
  });

  describe('Add Category', () => {
    it('should open form modal when add button is clicked', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('新建分類')).toBeInTheDocument();
      });

      const addButton = screen.getByText('新建分類');
      fireEvent.click(addButton);

      await waitFor(() => {
        expect(screen.getByText('添加分類')).toBeInTheDocument();
      });
    });

    it('should open form modal when add child button is clicked', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      const addChildButtons = screen.getAllByText('增加子分類');
      const initialCount = addChildButtons.length;
      fireEvent.click(addChildButtons[0]);

      await waitFor(() => {
        // After Modal opens, there should be one more "增加子分類" (Modal title)
        const allMatches = screen.getAllByText('增加子分類');
        expect(allMatches.length).toBeGreaterThan(initialCount);
      });
    });
  });

  describe('Edit Category', () => {
    it('should open form modal when edit button is clicked', async () => {
      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      const editButtons = screen.getAllByText('編輯');
      fireEvent.click(editButtons[0]);

      await waitFor(() => {
        expect(screen.getByText('編輯分類')).toBeInTheDocument();
      });
    });
  });

  describe('Delete Category', () => {
    it('should show confirm modal when delete button is clicked', async () => {
      const confirmSpy = vi.spyOn(Modal, 'confirm');

      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByText('刪除');
      fireEvent.click(deleteButtons[0]);

      await waitFor(() => {
        expect(confirmSpy).toHaveBeenCalledWith(
          expect.objectContaining({
            title: '確認刪除？',
            content: '刪除後無法恢復，請確認是否刪除該分類。',
          })
        );
      });

      confirmSpy.mockRestore();
    });

    it('should call delete API when confirm is clicked', async () => {
      let onOkCallback: any;
      const confirmSpy = vi.spyOn(Modal, 'confirm').mockImplementation((config: any) => {
        onOkCallback = config.onOk;
        return {} as any;
      });

      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      // 點擊刪除按鈕
      const deleteButtons = screen.getAllByText('刪除');
      fireEvent.click(deleteButtons[0]);

      await waitFor(() => {
        expect(confirmSpy).toHaveBeenCalled();
      });

      // 執行 Modal.confirm 的 onOk 回調
      await onOkCallback();

      await waitFor(() => {
        expect(categoryApi.deleteCategory).toHaveBeenCalledWith(1);
        expect(message.success).toHaveBeenCalledWith('刪除成功');
      });

      confirmSpy.mockRestore();
    });

    it('should not delete when cancel is clicked', async () => {
      let onCancelCallback: any;
      const confirmSpy = vi.spyOn(Modal, 'confirm').mockImplementation((config: any) => {
        onCancelCallback = config.onCancel;
        return {} as any;
      });

      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      // 點擊刪除按鈕
      const deleteButtons = screen.getAllByText('刪除');
      fireEvent.click(deleteButtons[0]);

      await waitFor(() => {
        expect(confirmSpy).toHaveBeenCalled();
      });

      // 執行 Modal.confirm 的 onCancel 回調 (如果存在)
      if (onCancelCallback) {
        await onCancelCallback();
      }

      // 驗證沒有調用刪除 API
      expect(categoryApi.deleteCategory).not.toHaveBeenCalled();

      confirmSpy.mockRestore();
    });
  });

  describe('Error Handling', () => {
    it('should show error message when query fails', async () => {
      vi.mocked(categoryApi.queryCategoryTree).mockRejectedValueOnce(new Error('Network Error'));

      renderCategoryManagement();

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('加載分類數據失敗');
      });
    });

    it('should show error message when delete fails', async () => {
      vi.mocked(categoryApi.deleteCategory).mockRejectedValueOnce(new Error('Delete Error'));

      let onOkCallback: any;
      const confirmSpy = vi.spyOn(Modal, 'confirm').mockImplementation((config: any) => {
        onOkCallback = config.onOk;
        return {} as any;
      });

      renderCategoryManagement();

      await waitFor(() => {
        expect(screen.getByText('電子產品')).toBeInTheDocument();
      });

      // 點擊刪除按鈕
      const deleteButtons = screen.getAllByText('刪除');
      fireEvent.click(deleteButtons[0]);

      await waitFor(() => {
        expect(confirmSpy).toHaveBeenCalled();
      });

      // 執行 Modal.confirm 的 onOk 回調
      await onOkCallback();

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('刪除失敗');
      });

      confirmSpy.mockRestore();
    });
  });
});
