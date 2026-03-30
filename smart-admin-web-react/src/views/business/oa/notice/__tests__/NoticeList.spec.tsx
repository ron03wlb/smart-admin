import { screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import NoticeList from '../NoticeList';

// Mock RichTextEditor to avoid heavy Tiptap initialization in jsdom
vi.mock('@/components/RichTextEditor/RichTextEditor', () => ({
  default: () => <textarea data-testid="rich-editor" />,
}));

// Mock NoticeFormDrawer to avoid transitive heavy imports
vi.mock('../NoticeFormDrawer', () => ({
  default: () => null,
}));

const mockNotices = [
  { noticeId: 1, noticeTypeId: 1, noticeTypeName: '通知', title: '系统维护公告', allVisibleFlag: true, author: '管理员', createTime: '2025-01-01', pageViewCount: 100 },
  { noticeId: 2, noticeTypeId: 2, noticeTypeName: '公告', title: '放假通知', allVisibleFlag: false, author: '人事部', createTime: '2025-01-02', pageViewCount: 50 },
];

const mockTypes = [
  { noticeTypeId: 1, noticeTypeName: '通知' },
  { noticeTypeId: 2, noticeTypeName: '公告' },
];

const mockQuery = vi.fn();
const mockGetAll = vi.fn();

vi.mock('@/api/business/oa/notice-api', () => ({
  noticeApi: {
    query: (...args: unknown[]) => mockQuery(...args),
    add: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getUpdateVO: vi.fn(),
  },
  noticeTypeApi: {
    getAll: (...args: unknown[]) => mockGetAll(...args),
    add: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('NoticeList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockQuery.mockResolvedValue({
      code: 1,
      data: { list: mockNotices, total: 2 },
      success: true,
    });
    mockGetAll.mockResolvedValue({
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

    expect(mockQuery).toHaveBeenCalled();
  });

  it('should display visibility tags', async () => {
    renderWithProviders(<NoticeList />);

    await waitFor(() => {
      expect(screen.getByText('全部')).toBeInTheDocument();
      expect(screen.getByText('部分')).toBeInTheDocument();
    });
  });
});
