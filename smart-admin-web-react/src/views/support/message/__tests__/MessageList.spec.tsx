import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import MessageList from '../MessageList';
import { messageApi } from '@/api/support/message-api';

vi.mock('@/api/support/message-api');
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    query: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 } }),
    queryAll: vi.fn().mockResolvedValue({ code: 1, data: [] }),
  },
}));

const mockMessages = [
  { messageId: 1, title: '系统通知', content: '欢迎使用', messageTypeName: '通知', readFlag: false, createTime: '2024-01-01 10:00:00' },
  { messageId: 2, title: '版本更新', content: '新版本发布', messageTypeName: '公告', readFlag: true, createTime: '2024-01-02 10:00:00' },
];

describe('MessageList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(messageApi.query).mockResolvedValue({
      code: 1,
      data: { list: mockMessages, total: 2 },
      success: true,
    });
  });

  it('should display message list on mount', async () => {
    renderWithProviders(<MessageList />);

    await waitFor(() => {
      expect(screen.getByText('系统通知')).toBeInTheDocument();
      expect(screen.getByText('版本更新')).toBeInTheDocument();
    });

    expect(messageApi.query).toHaveBeenCalled();
  });

  it('should have send message button', async () => {
    renderWithProviders(<MessageList />);

    await waitFor(() => {
      expect(screen.getByText('系统通知')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /发\s*送\s*消\s*息/ })).toBeInTheDocument();
  });
});
