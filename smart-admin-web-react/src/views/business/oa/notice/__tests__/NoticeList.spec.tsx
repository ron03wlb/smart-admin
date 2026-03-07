import { screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import NoticeList from '../NoticeList';
import { noticeApi, noticeTypeApi } from '@/api/business/oa/notice-api';

vi.mock('@/api/business/oa/notice-api');

const mockNotices = [
  { noticeId: 1, noticeTypeId: 1, noticeTypeName: '通知', title: '系统维护公告', allVisibleFlag: true, author: '管理员', createTime: '2025-01-01', pageViewCount: 100 },
  { noticeId: 2, noticeTypeId: 2, noticeTypeName: '公告', title: '放假通知', allVisibleFlag: false, author: '人事部', createTime: '2025-01-02', pageViewCount: 50 },
];

const mockTypes = [
  { noticeTypeId: 1, noticeTypeName: '通知' },
  { noticeTypeId: 2, noticeTypeName: '公告' },
];

describe('NoticeList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(noticeApi.query).mockResolvedValue({
      code: 1,
      data: { list: mockNotices, total: 2 },
      success: true,
    });
    vi.mocked(noticeTypeApi.getAll).mockResolvedValue({
      code: 1,
      data: mockTypes,
      success: true,
    });
  });

  it('should display notice list on mount', async () => {
    renderWithProviders(<NoticeList />);

    await waitFor(() => {
      expect(screen.getByText('系统维护公告')).toBeInTheDocument();
      expect(screen.getByText('放假通知')).toBeInTheDocument();
    });

    expect(noticeApi.query).toHaveBeenCalled();
  });

  it('should display visibility tags', async () => {
    renderWithProviders(<NoticeList />);

    await waitFor(() => {
      expect(screen.getByText('全部')).toBeInTheDocument();
      expect(screen.getByText('部分')).toBeInTheDocument();
    });
  });
});
