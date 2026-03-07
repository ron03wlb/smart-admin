import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import GoodsList from '../GoodsList';
import { goodsApi } from '@/api/business/erp/goods-api';

vi.mock('@/api/business/erp/goods-api');

const mockGoods = [
  { goodsId: 1, categoryId: 1, categoryName: '电子产品', goodsName: '笔记本电脑', goodsStatus: 1, place: '深圳', price: 5999, shelvesFlag: true, createTime: '2025-01-01' },
  { goodsId: 2, categoryId: 2, categoryName: '服装', goodsName: '运动鞋', goodsStatus: 2, place: '广州', price: 399, shelvesFlag: false, createTime: '2025-01-02' },
];

describe('GoodsList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(goodsApi.query).mockResolvedValue({
      code: 1,
      data: { list: mockGoods, total: 2 },
      success: true,
    });
  });

  it('should display goods list on mount', async () => {
    renderWithProviders(<GoodsList />);

    await waitFor(() => {
      expect(screen.getByText('笔记本电脑')).toBeInTheDocument();
      expect(screen.getByText('运动鞋')).toBeInTheDocument();
    });

    expect(goodsApi.query).toHaveBeenCalled();
  });

  it('should display goods status tags', async () => {
    renderWithProviders(<GoodsList />);

    await waitFor(() => {
      expect(screen.getByText('预约中')).toBeInTheDocument();
      expect(screen.getByText('禁用')).toBeInTheDocument();
    });
  });

  it('should open add drawer when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<GoodsList />);

    await waitFor(() => {
      expect(screen.getByText('笔记本电脑')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新\s*建/ });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加商品')).toBeInTheDocument();
    });
  });
});
