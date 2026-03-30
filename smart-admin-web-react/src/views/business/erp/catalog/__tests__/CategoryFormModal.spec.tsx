/**
 * CategoryFormModal Tests
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import CategoryFormModal from '../CategoryFormModal';

vi.mock('@/api/business/erp/catalog-api', () => ({
  categoryApi: {
    add: vi.fn().mockResolvedValue({ code: 1 }),
    update: vi.fn().mockResolvedValue({ code: 1 }),
  },
}));

import { categoryApi } from '@/api/business/erp/catalog-api';

describe('CategoryFormModal', () => {
  const onCancel = vi.fn();
  const onSuccess = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  it('should render add mode', async () => {
    render(
      <CategoryFormModal open={true} categoryType={1} parentId={0} onCancel={onCancel} onSuccess={onSuccess} />,
    );
    await waitFor(() => {
      expect(screen.getByText(/添加分类/)).toBeDefined();
    });
  });

  it('should render edit mode with category data', async () => {
    const category = { categoryId: 1, categoryName: '电子产品', sort: 1, categoryType: 1, parentId: 0 };
    render(
      <CategoryFormModal open={true} category={category} categoryType={1} parentId={0} onCancel={onCancel} onSuccess={onSuccess} />,
    );
    await waitFor(() => {
      expect(screen.getByDisplayValue('电子产品')).toBeDefined();
    });
  });

  it('should call add API for new category', async () => {
    render(
      <CategoryFormModal open={true} categoryType={1} parentId={0} onCancel={onCancel} onSuccess={onSuccess} />,
    );

    await waitFor(() => {
      expect(screen.getByLabelText('分类名称')).toBeDefined();
    });

    fireEvent.change(screen.getByLabelText('分类名称'), { target: { value: '新分类' } });
    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(categoryApi.add).toHaveBeenCalledWith(
        expect.objectContaining({ categoryName: '新分类' }),
      );
    });
  });

  it('should call update API for existing category', async () => {
    const category = { categoryId: 1, categoryName: '旧分类', sort: 1, categoryType: 1, parentId: 0 };
    render(
      <CategoryFormModal open={true} category={category} categoryType={1} parentId={0} onCancel={onCancel} onSuccess={onSuccess} />,
    );

    await waitFor(() => {
      expect(screen.getByDisplayValue('旧分类')).toBeDefined();
    });

    fireEvent.change(screen.getByDisplayValue('旧分类'), { target: { value: '更新分类' } });
    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(categoryApi.update).toHaveBeenCalledWith(
        expect.objectContaining({ categoryId: 1, categoryName: '更新分类' }),
      );
    });
  });
});
