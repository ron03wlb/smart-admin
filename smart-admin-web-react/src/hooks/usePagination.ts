/**
 * usePagination Hook — Standalone pagination state
 *
 * For cases where useTable is not needed (e.g., local data pagination).
 */
import { useState, useCallback } from 'react';
import type { TablePaginationConfig } from 'antd';
import { PAGE_SIZE } from '@/constants/common-const';

export interface UsePaginationReturn {
  pagination: TablePaginationConfig;
  pageNum: number;
  pageSize: number;
  setPageNum: (page: number) => void;
  setPageSize: (size: number) => void;
  setTotal: (total: number) => void;
  reset: () => void;
  onTableChange: (pagination: TablePaginationConfig) => void;
}

export function usePagination(defaultPageSize = PAGE_SIZE): UsePaginationReturn {
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [total, setTotal] = useState(0);

  const reset = useCallback(() => {
    setPageNum(1);
    setPageSize(defaultPageSize);
  }, [defaultPageSize]);

  const onTableChange = useCallback((pag: TablePaginationConfig) => {
    if (pag.current) setPageNum(pag.current);
    if (pag.pageSize) setPageSize(pag.pageSize);
  }, []);

  const pagination: TablePaginationConfig = {
    current: pageNum,
    pageSize,
    total,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (t) => `共 ${t} 条`,
  };

  return {
    pagination,
    pageNum,
    pageSize,
    setPageNum,
    setPageSize,
    setTotal,
    reset,
    onTableChange,
  };
}
