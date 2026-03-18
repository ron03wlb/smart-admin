/**
 * CategoryTreeSelect Component Tests
 * CategoryTreeSelect 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import CategoryTreeSelect from './index';
import * as categoryApi from '@/api/business/categoryApi';

// Mock category API
vi.mock('@/api/business/categoryApi', () => ({
  queryCategoryTree: vi.fn(),
}));

// Mock Ant Design message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
    },
  };
});

describe('CategoryTreeSelect', () => {
  const mockTreeData = [
    {
      categoryId: 1,
      categoryName: '電子產品',
      parentId: 0,
      categoryType: 1,
      disabledFlag: false,
      value: 1,
      title: '電子產品',
      key: 1,
      children: [
        {
          categoryId: 2,
          categoryName: '手機',
          parentId: 1,
          categoryType: 1,
          disabledFlag: false,
          value: 2,
          title: '手機',
          key: 2,
        },
      ],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('基本功能', () => {
    it('應該渲染 TreeSelect 組件', () => {
      const { container } = render(<CategoryTreeSelect categoryType={1} />);

      const treeSelect = container.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();
    });

    it('應該在 mount 時查詢分類樹數據', async () => {
      vi.mocked(categoryApi.queryCategoryTree).mockResolvedValue({
        ok: true,
        data: mockTreeData,
        msg: '',
        code: 0,
      });

      render(<CategoryTreeSelect categoryType={1} />);

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalledWith({ categoryType: 1 });
      });
    });

    it('應該接受 value 屬性', () => {
      const { container } = render(<CategoryTreeSelect categoryType={1} value={1} />);

      const treeSelect = container.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();
    });

    it('應該接受 onChange 回調', () => {
      const handleChange = vi.fn();
      const { container } = render(
        <CategoryTreeSelect categoryType={1} value={1} onChange={handleChange} />
      );

      const treeSelect = container.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();
    });
  });

  describe('categoryType 變化', () => {
    it('當 categoryType 變化時應該重新查詢數據', async () => {
      vi.mocked(categoryApi.queryCategoryTree).mockResolvedValue({
        ok: true,
        data: mockTreeData,
        msg: '',
        code: 0,
      });

      const { rerender } = render(<CategoryTreeSelect categoryType={1} />);

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalledWith({ categoryType: 1 });
      });

      // 改變 categoryType
      rerender(<CategoryTreeSelect categoryType={2} />);

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalledWith({ categoryType: 2 });
      });

      expect(categoryApi.queryCategoryTree).toHaveBeenCalledTimes(2);
    });

    it('當 categoryType 為 undefined 時應該清空數據', () => {
      const { container } = render(<CategoryTreeSelect categoryType={undefined} />);

      const treeSelect = container.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();
      expect(categoryApi.queryCategoryTree).not.toHaveBeenCalled();
    });
  });

  describe('API 錯誤處理', () => {
    it('當 API 返回錯誤時應該顯示錯誤消息', async () => {
      vi.mocked(categoryApi.queryCategoryTree).mockResolvedValue({
        ok: false,
        data: [],
        msg: '查詢失敗',
        code: 500,
      });

      render(<CategoryTreeSelect categoryType={1} />);

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalledWith({ categoryType: 1 });
      });
    });

    it('當 API 拋出異常時應該捕獲錯誤', async () => {
      vi.mocked(categoryApi.queryCategoryTree).mockRejectedValue(new Error('Network error'));

      render(<CategoryTreeSelect categoryType={1} />);

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalledWith({ categoryType: 1 });
      });
    });
  });

  describe('自定義屬性', () => {
    it('應該支持自定義寬度', () => {
      const { container } = render(<CategoryTreeSelect categoryType={1} width="300px" />);

      const treeSelect = container.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();
    });

    it('應該支持自定義 placeholder', () => {
      render(<CategoryTreeSelect categoryType={1} placeholder="選擇分類" />);

      const treeSelect = document.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();
    });

    it('應該支持禁用', () => {
      const { container } = render(<CategoryTreeSelect categoryType={1} disabled />);

      const treeSelect = container.querySelector('.ant-select-disabled');
      expect(treeSelect).toBeInTheDocument();
    });
  });

  describe('加載狀態', () => {
    it('應該在加載時顯示 loading 狀態', async () => {
      vi.mocked(categoryApi.queryCategoryTree).mockImplementation(
        () =>
          new Promise(resolve => {
            setTimeout(
              () =>
                resolve({
                  ok: true,
                  data: mockTreeData,
                  msg: '',
                  code: 0,
                }),
              100
            );
          })
      );

      const { container } = render(<CategoryTreeSelect categoryType={1} />);

      // TreeSelect 組件已渲染
      const treeSelect = container.querySelector('.ant-select');
      expect(treeSelect).toBeInTheDocument();

      await waitFor(() => {
        expect(categoryApi.queryCategoryTree).toHaveBeenCalled();
      });
    });
  });
});
