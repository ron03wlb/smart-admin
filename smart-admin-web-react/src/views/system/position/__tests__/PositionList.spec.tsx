import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import PositionList from '../PositionList';
import { positionApi } from '@/api/system/position-api';

vi.mock('@/api/system/position-api');

const mockPositions = [
  { positionId: 1, positionName: '高级工程师', positionLevel: 'P7', sort: 1, remark: '技术岗', createTime: '2026-01-01' },
  { positionId: 2, positionName: '产品经理', positionLevel: 'P6', sort: 2, remark: '产品岗', createTime: '2026-01-02' },
];

describe('PositionList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(positionApi.queryPage).mockResolvedValue({
      code: 1,
      data: { list: mockPositions, total: 2 },
      success: true,
    });
  });

  it('should fetch and display position data on mount', async () => {
    renderWithProviders(<PositionList />);

    await waitFor(() => {
      expect(screen.getByText('高级工程师')).toBeInTheDocument();
      expect(screen.getByText('产品经理')).toBeInTheDocument();
    });

    expect(positionApi.queryPage).toHaveBeenCalledWith(
      expect.objectContaining({ pageNum: 1, pageSize: 10 })
    );
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<PositionList />);

    await waitFor(() => {
      expect(screen.getByText('高级工程师')).toBeInTheDocument();
    });

    const addButton = screen.getByText('新建');
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加')).toBeInTheDocument();
    });
  });

  it('should render delete buttons for each row', async () => {
    renderWithProviders(<PositionList />);

    await waitFor(() => {
      expect(screen.getByText('高级工程师')).toBeInTheDocument();
    });

    const deleteButtons = screen.getAllByText('删除');
    expect(deleteButtons.length).toBe(2);
  });

  it('should handle search and reset', async () => {
    const user = userEvent.setup();
    renderWithProviders(<PositionList />);

    await waitFor(() => {
      expect(screen.getByText('高级工程师')).toBeInTheDocument();
    });

    const input = screen.getByPlaceholderText('关键字查询');
    await user.type(input, '工程师');

    const searchButton = screen.getByText('查询');
    await user.click(searchButton);

    await waitFor(() => {
      expect(positionApi.queryPage).toHaveBeenCalledWith(
        expect.objectContaining({ keywords: '工程师', pageNum: 1 })
      );
    });

    const resetButton = screen.getByText('重置');
    await user.click(resetButton);

    await waitFor(() => {
      expect(positionApi.queryPage).toHaveBeenCalledWith(
        expect.objectContaining({ keywords: '', pageNum: 1 })
      );
    });
  });
});
