import { screen, waitFor, fireEvent } from '@testing-library/react';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import GoodsList from '../GoodsList';

// Mock child drawer to avoid heavy rendering in jsdom
vi.mock('../GoodsFormModal', () => ({
  default: ({ open }: { open: boolean }) => open ? <div>添加商品</div> : null,
}));

const mockGoods = [
  { goodsId: 1, categoryId: 1, categoryName: '电子产品', goodsName: '笔记本电脑', goodsStatus: 1, place: '深圳', price: 5999, shelvesFlag: true, createTime: '2025-01-01' },
  { goodsId: 2, categoryId: 2, categoryName: '服装', goodsName: '运动鞋', goodsStatus: 2, place: '广州', price: 399, shelvesFlag: false, createTime: '2025-01-02' },
];

const mockQuery = vi.fn();

vi.mock('@/api/business/erp/goods-api', () => ({
  goodsApi: {
    query: (...args: unknown[]) => mockQuery(...args),
    add: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
    exportGoods: vi.fn(),
  },
}));

describe('GoodsList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockQuery.mockResolvedValue({
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

    expect(mockQuery).toHaveBeenCalled();
  });

  it('should display goods status tags', async () => {
    renderWithProviders(<GoodsList />);

    await waitFor(() => {
      expect(screen.getByText('预约中')).toBeInTheDocument();
      expect(screen.getByText('禁用')).toBeInTheDocument();
    });
  });

  it('should open add drawer when add button clicked', async () => {
    renderWithProviders(<GoodsList />);

    await waitFor(() => {
      expect(screen.getByText('笔记本电脑')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /新\s*建/ }));

    await waitFor(() => {
      expect(screen.getByText('添加商品')).toBeInTheDocument();
    });
  });
});
