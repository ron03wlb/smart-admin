import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useTable } from '../useTable';

interface MockItem {
  id: number;
  name: string;
}

const mockData: MockItem[] = [
  { id: 1, name: 'Alice' },
  { id: 2, name: 'Bob' },
  { id: 3, name: 'Charlie' },
];

/**
 * Create a mock API that returns SmartAdmin ResponseDTO format: { ok, data }
 */
function createMockApi(data: MockItem[] = mockData) {
  return vi.fn().mockResolvedValue({
    ok: true,
    data: {
      list: data,
      total: data.length,
    },
  });
}

describe('useTable', () => {
  let mockApi: ReturnType<typeof createMockApi>;

  beforeEach(() => {
    mockApi = createMockApi();
  });

  it('should have correct initial state', () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: false }),
    );

    expect(result.current.tableData).toEqual([]);
    expect(result.current.loading).toBe(false);
    expect(result.current.pagination.current).toBe(1);
    expect(result.current.pagination.pageSize).toBe(10);
    expect(result.current.selectedRowKeys).toEqual([]);
  });

  it('should auto-query on mount when autoQuery is true', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: true }),
    );

    await waitFor(() => {
      expect(result.current.tableData).toHaveLength(3);
    });

    expect(mockApi).toHaveBeenCalledTimes(1);
  });

  it('should not auto-query on mount when autoQuery is false', async () => {
    renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: false }),
    );

    // Give it time to potentially fire
    await new Promise((r) => setTimeout(r, 50));
    expect(mockApi).not.toHaveBeenCalled();
  });

  it('should call queryApi with correct params', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem, { keyword?: string }>({
        queryApi: mockApi,
        defaultQueryForm: { keyword: 'test' },
        autoQuery: true,
      }),
    );

    await waitFor(() => {
      expect(result.current.tableData).toHaveLength(3);
    });

    expect(mockApi).toHaveBeenCalledWith(
      expect.objectContaining({
        keyword: 'test',
        pageNum: 1,
        pageSize: 10,
      }),
    );
  });

  it('should handle page change via handleTableChange', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: true }),
    );

    await waitFor(() => {
      expect(result.current.tableData).toHaveLength(3);
    });

    act(() => {
      result.current.handleTableChange({ current: 2, pageSize: 10 }, {}, {});
    });

    await waitFor(() => {
      expect(mockApi).toHaveBeenCalledTimes(2);
    });

    expect(mockApi).toHaveBeenLastCalledWith(
      expect.objectContaining({ pageNum: 2, pageSize: 10 }),
    );
  });

  it('should handle pageSize change', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: true }),
    );

    await waitFor(() => {
      expect(result.current.tableData).toHaveLength(3);
    });

    act(() => {
      result.current.handleTableChange({ current: 1, pageSize: 20 }, {}, {});
    });

    await waitFor(() => {
      expect(mockApi).toHaveBeenCalledTimes(2);
    });

    expect(mockApi).toHaveBeenLastCalledWith(
      expect.objectContaining({ pageNum: 1, pageSize: 20 }),
    );
  });

  it('should reset query form and pagination', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem, { keyword?: string }>({
        queryApi: mockApi,
        defaultQueryForm: { keyword: '' },
        autoQuery: false,
      }),
    );

    act(() => {
      result.current.setQueryForm(prev => ({ ...prev, keyword: 'search' }));
    });

    expect(result.current.queryForm.keyword).toBe('search');

    act(() => {
      result.current.reset();
    });

    expect(result.current.queryForm.keyword).toBe('');
    expect(result.current.pagination.current).toBe(1);
  });

  it('should manage selectedRowKeys', () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: false }),
    );

    act(() => {
      result.current.setSelectedRowKeys([1, 2, 3]);
    });

    expect(result.current.selectedRowKeys).toEqual([1, 2, 3]);
  });

  it('should handle sort via handleTableChange', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, autoQuery: true }),
    );

    await waitFor(() => {
      expect(result.current.tableData).toHaveLength(3);
    });

    act(() => {
      result.current.handleTableChange(
        { current: 1, pageSize: 10 },
        {},
        { field: 'name', order: 'ascend' },
      );
    });

    await waitFor(() => {
      expect(mockApi).toHaveBeenCalledTimes(2);
    });

    expect(mockApi).toHaveBeenLastCalledWith(
      expect.objectContaining({
        sortItemList: [{ column: 'name', order: 'asc' }],
      }),
    );
  });

  it('should handle loading state during query', async () => {
    let resolvePromise: (value: any) => void;
    const slowApi = vi.fn().mockReturnValue(
      new Promise((resolve) => {
        resolvePromise = resolve;
      }),
    );

    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: slowApi, autoQuery: true }),
    );

    // Loading should be true during query
    await waitFor(() => {
      expect(result.current.loading).toBe(true);
    });

    // Resolve the promise
    act(() => {
      resolvePromise!({
        ok: true,
        data: { list: mockData, total: 3 },
      });
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });
});
