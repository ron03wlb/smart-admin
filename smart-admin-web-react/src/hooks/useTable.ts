/**
 * useTable Hook — Table data management with pagination
 *
 * Provides query, pagination, sorting, row selection for CRUD pages.
 */
import { useState, useCallback, useEffect, useRef } from 'react';
import type { TablePaginationConfig } from 'antd';
import type { Key } from 'react';
import type { PageParam, PageResult, SortItem } from '@/api/base/page.model';
import type { ResponseModel } from '@/api/base/response.model';
import { PAGE_SIZE } from '@/constants/common-const';

export interface UseTableOptions<T, Q extends Record<string, any> = Record<string, any>> {
  queryApi: (params: Q & PageParam) => Promise<ResponseModel<PageResult<T>>>;
  defaultQueryForm?: Partial<Q>;
  defaultPageSize?: number;
  immediate?: boolean;
}

export interface UseTableReturn<T, Q extends Record<string, any>> {
  dataSource: T[];
  loading: boolean;
  total: number;
  pagination: TablePaginationConfig;
  queryForm: Q;
  setQueryForm: (form: Partial<Q>) => void;
  query: () => Promise<void>;
  resetQuery: () => void;
  onTableChange: (pagination: TablePaginationConfig, _filters?: any, sorter?: any) => void;
  selectedRowKeys: Key[];
  setSelectedRowKeys: (keys: Key[]) => void;
}

export function useTable<T, Q extends Record<string, any> = Record<string, any>>(
  options: UseTableOptions<T, Q>,
): UseTableReturn<T, Q> {
  const { queryApi, defaultQueryForm, defaultPageSize = PAGE_SIZE, immediate = true } = options;

  const [dataSource, setDataSource] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [sortItemList, setSortItemList] = useState<SortItem[]>([]);
  const [queryForm, setQueryFormState] = useState<Q>((defaultQueryForm ?? {}) as Q);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);

  const isInitialMount = useRef(true);

  const query = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        ...queryForm,
        pageNum,
        pageSize,
        sortItemList: sortItemList.length > 0 ? sortItemList : undefined,
      } as Q & PageParam;

      const res = await queryApi(params);
      if (res.code === 1) {
        setDataSource(res.data?.list ?? []);
        setTotal(res.data?.total ?? 0);
      }
    } finally {
      setLoading(false);
    }
  }, [queryApi, queryForm, pageNum, pageSize, sortItemList]);

  const setQueryForm = useCallback((form: Partial<Q>) => {
    setQueryFormState((prev) => ({ ...prev, ...form }));
  }, []);

  const resetQuery = useCallback(() => {
    setQueryFormState((defaultQueryForm ?? {}) as Q);
    setPageNum(1);
    setSortItemList([]);
    setSelectedRowKeys([]);
  }, [defaultQueryForm]);

  const onTableChange = useCallback(
    (pag: TablePaginationConfig, _filters?: any, sorter?: any) => {
      if (pag.current) setPageNum(pag.current);
      if (pag.pageSize) setPageSize(pag.pageSize);

      // Handle sort
      if (sorter) {
        if (Array.isArray(sorter)) {
          setSortItemList(
            sorter
              .filter((s) => s.order)
              .map((s) => ({ column: s.field as string, isAsc: s.order === 'ascend' })),
          );
        } else if (sorter.order) {
          setSortItemList([{ column: sorter.field as string, isAsc: sorter.order === 'ascend' }]);
        } else {
          setSortItemList([]);
        }
      }
    },
    [],
  );

  const pagination: TablePaginationConfig = {
    current: pageNum,
    pageSize,
    total,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (t) => `共 ${t} 条`,
  };

  // Auto-query on mount if immediate
  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
      if (immediate) {
        query();
      }
      return;
    }
    query();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageNum, pageSize, sortItemList]);

  return {
    dataSource,
    loading,
    total,
    pagination,
    queryForm,
    setQueryForm,
    query,
    resetQuery,
    onTableChange,
    selectedRowKeys,
    setSelectedRowKeys,
  };
}
