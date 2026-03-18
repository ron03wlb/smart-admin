/**
 * useTable Hook
 * 表格邏輯 Hook
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/goods/goods-list.vue
 *
 * 功能：
 * - 分頁管理（pageNum, pageSize, total）
 * - 加載狀態管理
 * - 表格數據管理
 * - 查詢表單狀態管理
 * - 行選擇管理
 * - 排序管理
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { useState, useCallback, useEffect } from 'react';
import type { TablePaginationConfig } from 'antd';
import type { SorterResult } from 'antd/es/table/interface';

export interface PaginationConfig {
  /**
   * 當前頁碼（預設：1）
   */
  pageNum?: number;

  /**
   * 每頁條數（預設：10）
   */
  pageSize?: number;

  /**
   * 總記錄數
   */
  total?: number;
}

export interface SortItem {
  /**
   * 排序字段
   */
  column: string;

  /**
   * 排序方式（asc: 升序, desc: 降序）
   */
  order: 'asc' | 'desc';
}

export interface UseTableOptions<TData, TQueryForm> {
  /**
   * 查詢表單初始值
   */
  defaultQueryForm?: TQueryForm;

  /**
   * 分頁配置
   */
  pagination?: PaginationConfig;

  /**
   * 查詢數據的 API 函數
   * @param queryForm 查詢表單（包含分頁和排序信息）
   * @returns Promise<{ list: TData[], total: number }>
   */
  queryApi?: (queryForm: any) => Promise<{ data: { list: TData[]; total: number }; ok: boolean }>;

  /**
   * 是否在 mount 時自動查詢
   */
  autoQuery?: boolean;

  /**
   * 是否啟用行選擇
   */
  rowSelection?: boolean;
}

export interface UseTableResult<TData, TQueryForm> {
  /**
   * 表格數據
   */
  tableData: TData[];

  /**
   * 設置表格數據
   */
  setTableData: React.Dispatch<React.SetStateAction<TData[]>>;

  /**
   * 加載狀態
   */
  loading: boolean;

  /**
   * 設置加載狀態
   */
  setLoading: React.Dispatch<React.SetStateAction<boolean>>;

  /**
   * 查詢表單
   */
  queryForm: TQueryForm & PaginationConfig & { sortItemList?: SortItem[] };

  /**
   * 設置查詢表單
   */
  setQueryForm: React.Dispatch<
    React.SetStateAction<TQueryForm & PaginationConfig & { sortItemList?: SortItem[] }>
  >;

  /**
   * 分頁配置（Ant Design Table 格式）
   */
  pagination: TablePaginationConfig;

  /**
   * 選中的行 keys
   */
  selectedRowKeys: React.Key[];

  /**
   * 設置選中的行 keys
   */
  setSelectedRowKeys: React.Dispatch<React.SetStateAction<React.Key[]>>;

  /**
   * 查詢數據
   */
  query: () => Promise<void>;

  /**
   * 重置查詢表單
   */
  reset: () => void;

  /**
   * 處理分頁變化
   */
  handleTableChange: (pagination: TablePaginationConfig, filters: any, sorter: SorterResult<TData> | SorterResult<TData>[]) => void;

  /**
   * 處理行選擇變化
   */
  handleRowSelectionChange: (selectedRowKeys: React.Key[], selectedRows: TData[]) => void;
}

/**
 * 表格邏輯 Hook
 */
export function useTable<TData = any, TQueryForm extends Record<string, any> = Record<string, any>>(
  options: UseTableOptions<TData, TQueryForm> = {}
): UseTableResult<TData, TQueryForm> {
  const {
    defaultQueryForm = {} as TQueryForm,
    pagination: paginationConfig = {},
    queryApi,
    autoQuery = true,
    rowSelection: _rowSelection = false,
  } = options;

  const defaultPageNum = paginationConfig.pageNum || 1;
  const defaultPageSize = paginationConfig.pageSize || 10;

  // 表格數據
  const [tableData, setTableData] = useState<TData[]>([]);

  // 加載狀態
  const [loading, setLoading] = useState(false);

  // 查詢表單（包含分頁和排序）
  const [queryForm, setQueryForm] = useState<TQueryForm & PaginationConfig & { sortItemList?: SortItem[] }>({
    ...defaultQueryForm,
    pageNum: defaultPageNum,
    pageSize: defaultPageSize,
    sortItemList: [],
  });

  // 總記錄數
  const [total, setTotal] = useState(paginationConfig.total || 0);

  // 選中的行 keys
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  /**
   * 查詢數據
   */
  const query = useCallback(async () => {
    if (!queryApi) {
      console.warn('useTable: queryApi is not provided');
      return;
    }

    setLoading(true);
    try {
      const response = await queryApi(queryForm);

      if (response.ok && response.data) {
        setTableData(response.data.list || []);
        setTotal(response.data.total || 0);
      }
    } catch (error) {
      console.error('查詢數據失敗:', error);
      setTableData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [queryApi, queryForm]);

  /**
   * 重置查詢表單
   */
  const reset = useCallback(() => {
    const currentPageSize = queryForm.pageSize;
    setQueryForm({
      ...defaultQueryForm,
      pageNum: defaultPageNum,
      pageSize: currentPageSize,
      sortItemList: [],
    });
    setSelectedRowKeys([]);
  }, [defaultQueryForm, defaultPageNum, queryForm.pageSize]);

  /**
   * 處理表格變化（分頁、排序、篩選）
   */
  const handleTableChange = useCallback(
    (pagination: TablePaginationConfig, _filters: any, sorter: SorterResult<TData> | SorterResult<TData>[]) => {
      // 更新分頁
      const newQueryForm = {
        ...queryForm,
        pageNum: pagination.current || defaultPageNum,
        pageSize: pagination.pageSize || defaultPageSize,
      };

      // 處理排序
      if (sorter && !Array.isArray(sorter) && sorter.field && sorter.order) {
        newQueryForm.sortItemList = [
          {
            column: Array.isArray(sorter.field) ? sorter.field.join('.') : String(sorter.field),
            order: sorter.order === 'ascend' ? 'asc' : 'desc',
          },
        ];
      } else if (Array.isArray(sorter)) {
        newQueryForm.sortItemList = sorter
          .filter(s => s.field && s.order)
          .map(s => ({
            column: Array.isArray(s.field) ? s.field.join('.') : String(s.field),
            order: s.order === 'ascend' ? 'asc' : 'desc',
          }));
      } else {
        newQueryForm.sortItemList = [];
      }

      setQueryForm(newQueryForm);
    },
    [queryForm, defaultPageNum, defaultPageSize]
  );

  /**
   * 處理行選擇變化
   */
  const handleRowSelectionChange = useCallback((selectedRowKeys: React.Key[], _selectedRows: TData[]) => {
    setSelectedRowKeys(selectedRowKeys);
  }, []);

  /**
   * 組裝 Ant Design Table 分頁配置
   */
  const pagination: TablePaginationConfig = {
    current: queryForm.pageNum,
    pageSize: queryForm.pageSize,
    total: total,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 條`,
    pageSizeOptions: ['5', '10', '15', '20', '30', '40', '50', '75', '100'],
  };

  /**
   * 自動查詢
   */
  useEffect(() => {
    if (autoQuery && queryApi) {
      query();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * 當查詢表單變化時，自動查詢
   */
  useEffect(() => {
    if (queryApi && !autoQuery) {
      // 如果不是自動查詢，則不自動觸發
      return;
    }

    // 只在分頁或排序變化時查詢（避免初始化時重複查詢）
    const isFirstRender =
      queryForm.pageNum === defaultPageNum &&
      queryForm.pageSize === defaultPageSize &&
      (!queryForm.sortItemList || queryForm.sortItemList.length === 0);

    if (!isFirstRender && queryApi) {
      query();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryForm.pageNum, queryForm.pageSize, queryForm.sortItemList]);

  return {
    tableData,
    setTableData,
    loading,
    setLoading,
    queryForm,
    setQueryForm,
    pagination,
    selectedRowKeys,
    setSelectedRowKeys,
    query,
    reset,
    handleTableChange,
    handleRowSelectionChange,
  };
}
