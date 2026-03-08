/**
 * HeaderMessage Tests
 */
import { render, screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import HeaderMessage from '../HeaderMessage';

vi.mock('@/api/support/message-api', () => ({
  messageApi: {
    getUnreadCount: vi.fn().mockResolvedValue({ code: 1, data: 5 }),
    queryMessage: vi.fn().mockResolvedValue({
      code: 1,
      data: {
        list: [
          { messageId: 1, title: '系统通知', content: '测试内容', createTime: '2026-03-07 10:00:00', readFlag: false },
          { messageId: 2, title: '任务提醒', content: '任务内容', createTime: '2026-03-07 09:00:00', readFlag: false },
        ],
        total: 2,
      },
    }),
  },
}));

import { messageApi } from '@/api/support/message-api';

function renderHeaderMessage() {
  return render(
    <MemoryRouter>
      <HeaderMessage />
    </MemoryRouter>,
  );
}

describe('HeaderMessage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('should render bell icon', () => {
    renderHeaderMessage();
    expect(document.querySelector('.anticon-bell')).toBeDefined();
  });

  it('should fetch unread count on mount', async () => {
    renderHeaderMessage();

    await waitFor(() => {
      expect(messageApi.getUnreadCount).toHaveBeenCalled();
    });
  });

  it('should display unread badge count', async () => {
    renderHeaderMessage();

    await waitFor(() => {
      expect(screen.getByText('5')).toBeDefined();
    });
  });

  it('should render view more button in popover', async () => {
    renderHeaderMessage();

    // Click the bell to open popover
    const bell = document.querySelector('.anticon-bell')!;
    bell.closest('[class*="ant-badge"]')?.dispatchEvent(new MouseEvent('click', { bubbles: true }));

    // The "查看更多" button exists in the content
    await waitFor(() => {
      expect(screen.getByText(/查看更多/)).toBeDefined();
    });
  });
});
