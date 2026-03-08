/**
 * CodeGeneratorList Tests
 *
 * Tests list rendering, search, and pagination.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

// Mock highlight.js before any imports that use it
vi.mock('highlight.js/lib/core', () => ({
  default: {
    registerLanguage: vi.fn(),
    highlightElement: vi.fn(),
  },
}));
vi.mock('highlight.js/lib/languages/javascript', () => ({ default: vi.fn() }));
vi.mock('highlight.js/lib/languages/typescript', () => ({ default: vi.fn() }));
vi.mock('highlight.js/lib/languages/java', () => ({ default: vi.fn() }));
vi.mock('highlight.js/lib/languages/xml', () => ({ default: vi.fn() }));
vi.mock('highlight.js/styles/github-dark.css', () => ({}));

// Mock the API
vi.mock('@/api/support/code-generator-api', () => ({
  codeGeneratorApi: {
    queryTableList: vi.fn(),
    getTableColumns: vi.fn(),
    getConfig: vi.fn(),
    updateConfig: vi.fn(),
    preview: vi.fn(),
    downloadCode: vi.fn(),
  },
}));

// Mock DictSelect to avoid Redux dependency
vi.mock('@/components/support/dict-select/DictSelect', () => ({
  default: () => <div data-testid="dict-select" />,
}));

// Mock store hooks
vi.mock('@/store/hooks', () => ({
  useAppSelector: vi.fn(() => ({})),
  useAppDispatch: vi.fn(() => vi.fn()),
}));

import { codeGeneratorApi } from '@/api/support/code-generator-api';
import CodeGeneratorList from '../CodeGeneratorList';

const mockTableList = {
  success: true,
  data: {
    list: [
      { tableName: 't_employee', tableComment: '员工表', configTime: '2024-01-01 10:00:00' },
      { tableName: 't_department', tableComment: '部门表', configTime: null },
    ],
    total: 2,
  },
};

describe('CodeGeneratorList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (codeGeneratorApi.queryTableList as ReturnType<typeof vi.fn>).mockResolvedValue(mockTableList);
  });

  it('should render table with data', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      expect(screen.getByText('t_employee')).toBeDefined();
      expect(screen.getByText('t_department')).toBeDefined();
    });
  });

  it('should render search input and buttons', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('请输入表名关键字')).toBeDefined();
      expect(screen.getByText('查询')).toBeDefined();
      expect(screen.getByText('重置')).toBeDefined();
    });
  });

  it('should call API on mount', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      expect(codeGeneratorApi.queryTableList).toHaveBeenCalledWith({
        pageNum: 1,
        pageSize: 10,
        tableNameKeywords: undefined,
      });
    });
  });

  it('should render action buttons per row', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      const configButtons = screen.getAllByText('代码配置');
      const previewButtons = screen.getAllByText('代码预览');
      const downloadButtons = screen.getAllByText('下载代码');
      expect(configButtons.length).toBe(2);
      expect(previewButtons.length).toBe(2);
      expect(downloadButtons.length).toBe(2);
    });
  });

  it('should search with keywords', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      expect(screen.getByText('t_employee')).toBeDefined();
    });

    const searchInput = screen.getByPlaceholderText('请输入表名关键字');
    fireEvent.change(searchInput, { target: { value: 'employee' } });

    const searchBtn = screen.getByText('查询');
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(codeGeneratorApi.queryTableList).toHaveBeenCalledWith(
        expect.objectContaining({ tableNameKeywords: 'employee', pageNum: 1 }),
      );
    });
  });

  it('should reset query', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      expect(screen.getByText('t_employee')).toBeDefined();
    });

    const searchInput = screen.getByPlaceholderText('请输入表名关键字');
    fireEvent.change(searchInput, { target: { value: 'test' } });

    const resetBtn = screen.getByText('重置');
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(codeGeneratorApi.queryTableList).toHaveBeenCalledWith(
        expect.objectContaining({ tableNameKeywords: undefined, pageNum: 1 }),
      );
    });
  });

  it('should display total count', async () => {
    render(<CodeGeneratorList />);

    await waitFor(() => {
      expect(screen.getByText('共2条')).toBeDefined();
    });
  });
});
