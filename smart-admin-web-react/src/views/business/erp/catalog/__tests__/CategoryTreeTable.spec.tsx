/**
 * CategoryTreeTable Tests
 */
import { render, screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import CategoryTreeTable from '../CategoryTreeTable';

vi.mock('@/api/business/erp/catalog-api', () => ({
  categoryApi: {
    queryTree: vi.fn().mockResolvedValue({
      code: 1,
      data: [
        {
          categoryId: 1, categoryName: '电子产品', sort: 1, categoryType: 1, parentId: 0,
          children: [
            { categoryId: 2, categoryName: '手机', sort: 1, categoryType: 1, parentId: 1, children: [] },
          ],
        },
        { categoryId: 3, categoryName: '服装', sort: 2, categoryType: 1, parentId: 0, children: [] },
      ],
    }),
    delete: vi.fn().mockResolvedValue({ code: 1 }),
  },
}));

// Mock CategoryFormModal to avoid nested component issues
vi.mock('../CategoryFormModal', () => ({
  default: () => null,
}));

import { categoryApi } from '@/api/business/erp/catalog-api';

describe('CategoryTreeTable', () => {
  beforeEach(() => vi.clearAllMocks());

  it('should render tree table with data', async () => {
    render(<CategoryTreeTable categoryType={1} />);

    await waitFor(() => {
      expect(screen.getByText('电子产品')).toBeDefined();
      expect(screen.getByText('服装')).toBeDefined();
    }, { timeout: 10000 });
  });

  it('should call queryTree on mount', async () => {
    render(<CategoryTreeTable categoryType={1} />);

    await waitFor(() => {
      expect(categoryApi.queryTree).toHaveBeenCalledWith(
        expect.objectContaining({ categoryType: 1 }),
      );
    });
  });

  it('should render table columns', async () => {
    render(<CategoryTreeTable categoryType={1} />);

    await waitFor(() => {
      expect(screen.getByText('分类名称')).toBeDefined();
      expect(screen.getByText('排序')).toBeDefined();
      expect(screen.getByText('操作')).toBeDefined();
    });
  });
});
