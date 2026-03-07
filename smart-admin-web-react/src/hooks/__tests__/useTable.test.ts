import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useTable } from '../useTable';
import type { ResponseModel } from '@/api/base/response.model';
import type { PageResult } from '@/api/base/page.model';

interface MockItem {
  id: number;
  name: string;
}

const mockData: MockItem[] = [
  { id: 1, name: 'Alice' },
  { id: 2, name: 'Bob' },
  { id: 3, name: 'Charlie' },
];

function createMockApi(data: MockItem[] = mockData) {
  return vi.fn().mockResolvedValue({
    code: 1,
    success: true,
    data: {
      list: data,
      total: data.length,
    },
  } as ResponseModel<PageResult<MockItem>>);
}

describe('useTable', () => {
  let mockApi: ReturnType<typeof createMockApi>;

  beforeEach(() => {
    mockApi = createMockApi();
  });

  it('should have correct initial state', () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, immediate: false }),
    );

    expect(result.current.dataSource).toEqual([]);
    expect(result.current.loading).toBe(false);
    expect(result.current.total).toBe(0);
    expect(result.current.pagination.current).toBe(1);
    expect(result.current.pagination.pageSize).toBe(10);
    expect(result.current.selectedRowKeys).toEqual([]);
  });

  it('should auto-query on mount when immediate is true', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, immediate: true }),
    );

    await waitFor(() => {
      expect(result.current.dataSource).toHaveLength(3);
    });

    expect(mockApi).toHaveBeenCalledTimes(1);
    expect(result.current.total).toBe(3);
  });

  it('should not auto-query on mount when immediate is false', async () => {
    renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, immediate: false }),
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
        immediate: true,
      }),
    );

    await waitFor(() => {
      expect(result.current.dataSource).toHaveLength(3);
    });

    expect(mockApi).toHaveBeenCalledWith({
      keyword: 'test',
      pageNum: 1,
      pageSize: 10,
      sortItemList: undefined,
    });
  });

  it('should handle page change via onTableChange', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, immediate: true }),
    );

    await waitFor(() => {
      expect(result.current.dataSource).toHaveLength(3);
    });

    act(() => {
      result.current.onTableChange({ current: 2, pageSize: 10 });
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
      useTable<MockItem>({ queryApi: mockApi, immediate: true }),
    );

    await waitFor(() => {
      expect(result.current.dataSource).toHaveLength(3);
    });

    act(() => {
      result.current.onTableChange({ current: 1, pageSize: 20 });
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
        immediate: false,
      }),
    );

    act(() => {
      result.current.setQueryForm({ keyword: 'search' });
    });

    expect(result.current.queryForm.keyword).toBe('search');

    act(() => {
      result.current.resetQuery();
    });

    expect(result.current.queryForm.keyword).toBe('');
    expect(result.current.pagination.current).toBe(1);
  });

  it('should manage selectedRowKeys', () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, immediate: false }),
    );

    act(() => {
      result.current.setSelectedRowKeys([1, 2, 3]);
    });

    expect(result.current.selectedRowKeys).toEqual([1, 2, 3]);
  });

  it('should update queryForm via setQueryForm', () => {
    const { result } = renderHook(() =>
      useTable<MockItem, { keyword?: string; status?: number }>({
        queryApi: mockApi,
        defaultQueryForm: { keyword: '', status: undefined },
        immediate: false,
      }),
    );

    act(() => {
      result.current.setQueryForm({ keyword: 'hello', status: 1 });
    });

    expect(result.current.queryForm.keyword).toBe('hello');
    expect(result.current.queryForm.status).toBe(1);
  });

  it('should handle sort via onTableChange', async () => {
    const { result } = renderHook(() =>
      useTable<MockItem>({ queryApi: mockApi, immediate: true }),
    );

    await waitFor(() => {
      expect(result.current.dataSource).toHaveLength(3);
    });

    act(() => {
      result.current.onTableChange(
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
        sortItemList: [{ column: 'name', isAsc: true }],
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
      useTable<MockItem>({ queryApi: slowApi, immediate: true }),
    );

    // Loading should be true during query
    await waitFor(() => {
      expect(result.current.loading).toBe(true);
    });

    // Resolve the promise
    act(() => {
      resolvePromise!({
        code: 1,
        success: true,
        data: { list: mockData, total: 3 },
      });
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });
});
