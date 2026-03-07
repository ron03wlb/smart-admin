import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import DictIndex from '../DictIndex';
import { dictApi } from '@/api/support/dict-api';

vi.mock('@/api/support/dict-api');

const mockDicts = [
  { dictId: 1, dictCode: 'GENDER', dictName: '性别', remark: '', disabledFlag: false },
  { dictId: 2, dictCode: 'STATUS', dictName: '状态', remark: '启用/禁用', disabledFlag: false },
];

describe('DictIndex', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(dictApi.queryPage).mockResolvedValue({
      code: 1,
      data: { list: mockDicts, total: 2 },
      success: true,
    });
    vi.mocked(dictApi.queryDictData).mockResolvedValue({
      code: 1,
      data: [],
      success: true,
    });
  });

  it('should display dict list on mount', async () => {
    renderWithProviders(<DictIndex />);

    await waitFor(() => {
      expect(screen.getByText('GENDER')).toBeInTheDocument();
      expect(screen.getByText('STATUS')).toBeInTheDocument();
    });

    expect(dictApi.queryPage).toHaveBeenCalled();
  });

  it('should open add modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DictIndex />);

    await waitFor(() => {
      expect(screen.getByText('GENDER')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新\s*建/ });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加字典')).toBeInTheDocument();
    });
  });

  it('should open data drawer when dict code clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DictIndex />);

    await waitFor(() => {
      expect(screen.getByText('GENDER')).toBeInTheDocument();
    });

    await user.click(screen.getByText('GENDER'));

    await waitFor(() => {
      expect(screen.getByText('字典数据 [GENDER]')).toBeInTheDocument();
    });
  });
});
