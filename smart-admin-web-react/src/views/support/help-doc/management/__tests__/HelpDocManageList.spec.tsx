import { screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import HelpDocManageList from '../HelpDocManageList';
import { helpDocApi, helpDocCatalogApi } from '@/api/support/help-doc-api';

vi.mock('@/api/support/help-doc-api');

const mockCatalogs = [
  { helpDocCatalogId: 1, name: '开发文档', parentId: 0, sort: 0, children: [
    { helpDocCatalogId: 2, name: '前端文档', parentId: 1, sort: 0, children: [] },
  ]},
];

const mockDocs = [
  { helpDocId: 1, helpDocCatalogId: 1, helpDocCatalogName: '开发文档', title: 'React指南', author: '张三', sort: 0, pageViewCount: 10, userViewCount: 5, createTime: '2025-01-01' },
  { helpDocId: 2, helpDocCatalogId: 2, helpDocCatalogName: '前端文档', title: 'Vue迁移', author: '李四', sort: 1, pageViewCount: 20, userViewCount: 8, createTime: '2025-01-02' },
];

describe('HelpDocManageList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(helpDocCatalogApi.getAll).mockResolvedValue({
      code: 1,
      data: mockCatalogs,
      success: true,
    });
    vi.mocked(helpDocApi.query).mockResolvedValue({
      code: 1,
      data: { list: mockDocs, total: 2 },
      success: true,
    });
  });

  it('should display catalog tree on mount', async () => {
    renderWithProviders(<HelpDocManageList />);

    await waitFor(() => {
      expect(screen.getByText('开发文档')).toBeInTheDocument();
    });

    expect(helpDocCatalogApi.getAll).toHaveBeenCalled();
  });

  it('should display doc list on mount', async () => {
    renderWithProviders(<HelpDocManageList />);

    await waitFor(() => {
      expect(screen.getByText('React指南')).toBeInTheDocument();
      expect(screen.getByText('Vue迁移')).toBeInTheDocument();
    });

    expect(helpDocApi.query).toHaveBeenCalled();
  });

  it('should have search input for docs', async () => {
    renderWithProviders(<HelpDocManageList />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('标题/作者')).toBeInTheDocument();
    });
  });
});
