import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePagination } from '../usePagination';

describe('usePagination', () => {
  it('should have correct initial state', () => {
    const { result } = renderHook(() => usePagination());

    expect(result.current.pageNum).toBe(1);
    expect(result.current.pageSize).toBe(10);
    expect(result.current.pagination.total).toBe(0);
  });

  it('should accept custom default page size', () => {
    const { result } = renderHook(() => usePagination(20));

    expect(result.current.pageSize).toBe(20);
    expect(result.current.pagination.pageSize).toBe(20);
  });

  it('should update page number', () => {
    const { result } = renderHook(() => usePagination());

    act(() => {
      result.current.setPageNum(3);
    });

    expect(result.current.pageNum).toBe(3);
    expect(result.current.pagination.current).toBe(3);
  });

  it('should update page size', () => {
    const { result } = renderHook(() => usePagination());

    act(() => {
      result.current.setPageSize(50);
    });

    expect(result.current.pageSize).toBe(50);
    expect(result.current.pagination.pageSize).toBe(50);
  });

  it('should update total', () => {
    const { result } = renderHook(() => usePagination());

    act(() => {
      result.current.setTotal(100);
    });

    expect(result.current.pagination.total).toBe(100);
  });

  it('should handle onTableChange', () => {
    const { result } = renderHook(() => usePagination());

    act(() => {
      result.current.onTableChange({ current: 5, pageSize: 25 });
    });

    expect(result.current.pageNum).toBe(5);
    expect(result.current.pageSize).toBe(25);
  });

  it('should reset to defaults', () => {
    const { result } = renderHook(() => usePagination(15));

    act(() => {
      result.current.setPageNum(5);
      result.current.setPageSize(50);
    });

    expect(result.current.pageNum).toBe(5);

    act(() => {
      result.current.reset();
    });

    expect(result.current.pageNum).toBe(1);
    expect(result.current.pageSize).toBe(15);
  });
});
