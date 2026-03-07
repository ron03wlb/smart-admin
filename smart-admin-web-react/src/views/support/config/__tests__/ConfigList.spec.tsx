import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import ConfigList from '../ConfigList';
import { configApi } from '@/api/support/config-api';

vi.mock('@/api/support/config-api');

const mockConfigs = [
  { configId: 1, configKey: 'app.name', configName: '应用名称', configValue: 'SmartAdmin', remark: '', createTime: '2024-01-01', updateTime: '2024-01-01' },
  { configId: 2, configKey: 'app.version', configName: '版本号', configValue: '4.1.0', remark: '', createTime: '2024-01-01', updateTime: '2024-01-01' },
];

describe('ConfigList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(configApi.queryList).mockResolvedValue({
      code: 1,
      data: { list: mockConfigs, total: 2 },
      success: true,
    });
  });

  it('should display config list on mount', async () => {
    renderWithProviders(<ConfigList />);

    await waitFor(() => {
      expect(screen.getByText('app.name')).toBeInTheDocument();
      expect(screen.getByText('app.version')).toBeInTheDocument();
    });

    expect(configApi.queryList).toHaveBeenCalled();
  });

  it('should open add modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ConfigList />);

    await waitFor(() => {
      expect(screen.getByText('app.name')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新\s*建/ });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加参数')).toBeInTheDocument();
    });
  });

  it('should open edit modal when edit link clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ConfigList />);

    await waitFor(() => {
      expect(screen.getByText('app.name')).toBeInTheDocument();
    });

    const editLinks = screen.getAllByText('编辑');
    await user.click(editLinks[0]);

    await waitFor(() => {
      expect(screen.getByText('编辑参数')).toBeInTheDocument();
    });
  });
});
