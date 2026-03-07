import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import LoginLogList from '../LoginLogList';
import { loginLogApi } from '@/api/support/login-log-api';

vi.mock('@/api/support/login-log-api');

const mockLogs = [
  { loginLogId: 1, userId: 1, userName: 'admin', userAgent: 'Chrome', loginIp: '192.168.1.1', loginIpRegion: '局域网', loginType: 1, loginResult: 1, createTime: '2024-01-01 10:00:00' },
  { loginLogId: 2, userId: 2, userName: 'user1', userAgent: 'Firefox', loginIp: '10.0.0.1', loginIpRegion: '内网', loginType: 1, loginResult: 2, createTime: '2024-01-01 11:00:00' },
];

describe('LoginLogList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(loginLogApi.queryList).mockResolvedValue({
      code: 1,
      data: { list: mockLogs, total: 2 },
      success: true,
    });
  });

  it('should display login logs on mount', async () => {
    renderWithProviders(<LoginLogList />);

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
      expect(screen.getByText('user1')).toBeInTheDocument();
    });

    expect(loginLogApi.queryList).toHaveBeenCalled();
  });

  it('should display login result tags', async () => {
    renderWithProviders(<LoginLogList />);

    await waitFor(() => {
      expect(screen.getByText('登录成功')).toBeInTheDocument();
      expect(screen.getByText('登录失败')).toBeInTheDocument();
    });
  });

  it('should have search inputs for user and IP', async () => {
    renderWithProviders(<LoginLogList />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('用户名')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('IP')).toBeInTheDocument();
    });
  });

  it('should reset search on reset button click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginLogList />);

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const userInput = screen.getByPlaceholderText('用户名');
    await user.type(userInput, 'test');

    const resetButton = screen.getByRole('button', { name: /重\s*置/ });
    await user.click(resetButton);

    expect(userInput).toHaveValue('');
  });
});
