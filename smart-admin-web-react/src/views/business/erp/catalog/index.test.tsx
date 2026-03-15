/**
 * Goods Catalog Tests
 * 商品目錄測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import GoodsCatalog from './index';

// Mock API
vi.mock('@/api/business/categoryApi', () => ({
  categoryApi: {
    queryCategoryTree: vi.fn().mockResolvedValue({
      data: [
        {
          categoryId: 1,
          categoryName: '電子產品',
          categoryType: 1,
          parentId: undefined,
          disabledFlag: false,
          children: [
            {
              categoryId: 2,
              categoryName: '手機',
              categoryType: 1,
              parentId: 1,
              disabledFlag: false,
            },
            {
              categoryId: 3,
              categoryName: '電腦',
              categoryType: 1,
              parentId: 1,
              disabledFlag: false,
            },
          ],
        },
        {
          categoryId: 4,
          categoryName: '服裝',
          categoryType: 1,
          parentId: undefined,
          disabledFlag: false,
        },
      ],
    }),
    addCategory: vi.fn().mockResolvedValue({ data: null }),
    updateCategory: vi.fn().mockResolvedValue({ data: null }),
    deleteCategory: vi.fn().mockResolvedValue({ data: null }),
  },
}));

// Mock hooks
vi.mock('@/hooks/usePrivilege', () => ({
  usePrivilege: vi.fn(() => true), // 默認有權限
}));

describe('GoodsCatalog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('應該正常渲染商品目錄頁面', async () => {
    render(
      <BrowserRouter>
        <GoodsCatalog />
      </BrowserRouter>
    );

    // 檢查標題
    expect(screen.getByText('商品目錄')).toBeInTheDocument();

    // 檢查新建按鈕
    expect(screen.getByText('新建')).toBeInTheDocument();
  });

  it('應該加載並顯示分類數據（樹形結構）', async () => {
    const { container } = render(
      <BrowserRouter>
        <GoodsCatalog />
      </BrowserRouter>
    );

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('電子產品')).toBeInTheDocument();
    });

    // 檢查根分類
    expect(screen.getByText('電子產品')).toBeInTheDocument();
    expect(screen.getByText('服裝')).toBeInTheDocument();

    // 檢查是否有展開按鈕（樹形表格特性）
    const expandButtons = container.querySelectorAll('.ant-table-row-expand-icon');
    expect(expandButtons.length).toBeGreaterThan(0);
  });

  it('應該顯示操作按鈕（增加子分類、編輯、刪除）', async () => {
    render(
      <BrowserRouter>
        <GoodsCatalog />
      </BrowserRouter>
    );

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('電子產品')).toBeInTheDocument();
    });

    // 檢查操作按鈕（每個分類都有這些按鈕）
    const addChildButtons = screen.getAllByText('增加子分類');
    const editButtons = screen.getAllByText('編輯');
    const deleteButtons = screen.getAllByText('刪除');

    expect(addChildButtons.length).toBeGreaterThan(0);
    expect(editButtons.length).toBeGreaterThan(0);
    expect(deleteButtons.length).toBeGreaterThan(0);
  });
});
