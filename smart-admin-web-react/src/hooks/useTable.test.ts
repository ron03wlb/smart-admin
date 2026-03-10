/**
 * useTable Hook Tests
 * useTable Hook 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useTable } from './useTable';
import type { UseTableOptions } from './useTable';

describe('useTable', () => {
  // Mock API 函數
  const mockQueryApi = vi.fn();

  const mockData = {
    list: [
      { id: 1, name: '商品1' },
      { id: 2, name: '商品2' },
      { id: 3, name: '商品3' },
    ],
    total: 30,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockQueryApi.mockResolvedValue({
      ok: true,
      data: mockData,
    });
  });

  describe('基本功能', () => {
    it('應該初始化默認狀態', () => {
      const { result } = renderHook(() => useTable({ autoQuery: false }));

      expect(result.current.tableData).toEqual([]);
      expect(result.current.loading).toBe(false);
      expect(result.current.queryForm.pageNum).toBe(1);
      expect(result.current.queryForm.pageSize).toBe(10);
      expect(result.current.selectedRowKeys).toEqual([]);
    });

    it('應該接受自定義分頁配置', () => {
      const { result } = renderHook(() =>
        useTable({
          autoQuery: false,
          pagination: {
            pageNum: 2,
            pageSize: 20,
          },
        })
      );

      expect(result.current.queryForm.pageNum).toBe(2);
      expect(result.current.queryForm.pageSize).toBe(20);
    });

    it('應該接受自定義查詢表單初始值', () => {
      const defaultQueryForm = {
        name: '商品',
        status: 1,
      };

      const { result } = renderHook(() =>
        useTable({
          autoQuery: false,
          defaultQueryForm,
        })
      );

      expect(result.current.queryForm).toMatchObject(defaultQueryForm);
    });
  });

  describe('自動查詢', () => {
    it('應該在 mount 時自動查詢（autoQuery=true）', async () => {
      renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: true,
        })
      );

      await waitFor(() => {
        expect(mockQueryApi).toHaveBeenCalledTimes(1);
      });
    });

    it('應該在 mount 時不自動查詢（autoQuery=false）', async () => {
      renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      await waitFor(() => {
        expect(mockQueryApi).not.toHaveBeenCalled();
      });
    });
  });

  describe('query 方法', () => {
    it('應該正確查詢數據並更新狀態', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      await act(async () => {
        await result.current.query();
      });

      await waitFor(() => {
        expect(result.current.tableData).toEqual(mockData.list);
        expect(result.current.pagination.total).toBe(mockData.total);
      });
    });

    it('應該在查詢時設置 loading 狀態', async () => {
      let resolveQuery: any;
      const delayedQueryApi = vi.fn(
        () =>
          new Promise(resolve => {
            resolveQuery = resolve;
          })
      );

      const { result } = renderHook(() =>
        useTable({
          queryApi: delayedQueryApi,
          autoQuery: false,
        })
      );

      // 開始查詢
      act(() => {
        result.current.query();
      });

      // loading 應該為 true
      expect(result.current.loading).toBe(true);

      // 完成查詢
      await act(async () => {
        resolveQuery({ ok: true, data: mockData });
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });
    });

    it('應該處理查詢錯誤', async () => {
      const errorApi = vi.fn().mockRejectedValue(new Error('Network error'));
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const { result } = renderHook(() =>
        useTable({
          queryApi: errorApi,
          autoQuery: false,
        })
      );

      await act(async () => {
        await result.current.query();
      });

      await waitFor(() => {
        expect(result.current.tableData).toEqual([]);
        expect(result.current.pagination.total).toBe(0);
        expect(consoleErrorSpy).toHaveBeenCalled();
      });

      consoleErrorSpy.mockRestore();
    });

    it('應該處理 API 未提供的情況', async () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const { result } = renderHook(() =>
        useTable({
          autoQuery: false,
        })
      );

      await act(async () => {
        await result.current.query();
      });

      expect(consoleWarnSpy).toHaveBeenCalledWith('useTable: queryApi is not provided');

      consoleWarnSpy.mockRestore();
    });
  });

  describe('reset 方法', () => {
    it('應該重置查詢表單到初始值', async () => {
      const defaultQueryForm = {
        name: '商品',
        status: 1,
      };

      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
          defaultQueryForm,
        })
      );

      // 修改查詢表單
      act(() => {
        result.current.setQueryForm({
          ...result.current.queryForm,
          name: '新商品',
          status: 2,
          pageNum: 3,
        });
      });

      // 重置
      act(() => {
        result.current.reset();
      });

      await waitFor(() => {
        expect(result.current.queryForm.name).toBe('商品');
        expect(result.current.queryForm.status).toBe(1);
        expect(result.current.queryForm.pageNum).toBe(1);
      });
    });

    it('應該保留當前 pageSize', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      // 修改 pageSize
      act(() => {
        result.current.setQueryForm({
          ...result.current.queryForm,
          pageSize: 20,
        });
      });

      // 重置
      act(() => {
        result.current.reset();
      });

      await waitFor(() => {
        expect(result.current.queryForm.pageSize).toBe(20);
      });
    });

    it('應該清空選中的行', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
          rowSelection: true,
        })
      );

      // 選中行
      act(() => {
        result.current.setSelectedRowKeys([1, 2, 3]);
      });

      // 重置
      act(() => {
        result.current.reset();
      });

      await waitFor(() => {
        expect(result.current.selectedRowKeys).toEqual([]);
      });
    });
  });

  describe('分頁處理', () => {
    it('應該處理分頁變化', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.handleTableChange(
          {
            current: 2,
            pageSize: 20,
          },
          {},
          {} as any
        );
      });

      await waitFor(() => {
        expect(result.current.queryForm.pageNum).toBe(2);
        expect(result.current.queryForm.pageSize).toBe(20);
      });
    });

    it('應該返回正確的 Ant Design 分頁配置', () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
          pagination: {
            pageNum: 2,
            pageSize: 20,
            total: 100,
          },
        })
      );

      expect(result.current.pagination).toMatchObject({
        current: 2,
        pageSize: 20,
        total: 100,
        showSizeChanger: true,
        showQuickJumper: true,
      });
      expect(result.current.pagination.showTotal).toBeDefined();
      expect(result.current.pagination.showTotal!(100)).toBe('共 100 條');
    });
  });

  describe('排序處理', () => {
    it('應該處理單列排序（升序）', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.handleTableChange(
          { current: 1, pageSize: 10 },
          {},
          {
            field: 'price',
            order: 'ascend',
          } as any
        );
      });

      await waitFor(() => {
        expect(result.current.queryForm.sortItemList).toEqual([
          {
            column: 'price',
            order: 'asc',
          },
        ]);
      });
    });

    it('應該處理單列排序（降序）', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.handleTableChange(
          { current: 1, pageSize: 10 },
          {},
          {
            field: 'createTime',
            order: 'descend',
          } as any
        );
      });

      await waitFor(() => {
        expect(result.current.queryForm.sortItemList).toEqual([
          {
            column: 'createTime',
            order: 'desc',
          },
        ]);
      });
    });

    it('應該處理多列排序', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.handleTableChange(
          { current: 1, pageSize: 10 },
          {},
          [
            { field: 'price', order: 'ascend' },
            { field: 'createTime', order: 'descend' },
          ] as any
        );
      });

      await waitFor(() => {
        expect(result.current.queryForm.sortItemList).toEqual([
          { column: 'price', order: 'asc' },
          { column: 'createTime', order: 'desc' },
        ]);
      });
    });

    it('應該處理取消排序', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      // 先設置排序
      act(() => {
        result.current.handleTableChange(
          { current: 1, pageSize: 10 },
          {},
          {
            field: 'price',
            order: 'ascend',
          } as any
        );
      });

      // 取消排序
      act(() => {
        result.current.handleTableChange({ current: 1, pageSize: 10 }, {}, {} as any);
      });

      await waitFor(() => {
        expect(result.current.queryForm.sortItemList).toEqual([]);
      });
    });

    it('應該處理數組字段排序', async () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.handleTableChange(
          { current: 1, pageSize: 10 },
          {},
          {
            field: ['user', 'name'],
            order: 'ascend',
          } as any
        );
      });

      await waitFor(() => {
        expect(result.current.queryForm.sortItemList).toEqual([
          {
            column: 'user.name',
            order: 'asc',
          },
        ]);
      });
    });
  });

  describe('行選擇處理', () => {
    it('應該處理行選擇變化', () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
          rowSelection: true,
        })
      );

      act(() => {
        result.current.handleRowSelectionChange([1, 2, 3], [] as any);
      });

      expect(result.current.selectedRowKeys).toEqual([1, 2, 3]);
    });

    it('應該清空行選擇', () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
          rowSelection: true,
        })
      );

      // 選中行
      act(() => {
        result.current.setSelectedRowKeys([1, 2, 3]);
      });

      // 清空選擇
      act(() => {
        result.current.handleRowSelectionChange([], [] as any);
      });

      expect(result.current.selectedRowKeys).toEqual([]);
    });
  });

  describe('狀態更新', () => {
    it('應該允許手動設置表格數據', () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      const newData = [{ id: 1, name: '新商品' }];

      act(() => {
        result.current.setTableData(newData);
      });

      expect(result.current.tableData).toEqual(newData);
    });

    it('應該允許手動設置加載狀態', () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.setLoading(true);
      });

      expect(result.current.loading).toBe(true);
    });

    it('應該允許手動設置查詢表單', () => {
      const { result } = renderHook(() =>
        useTable({
          queryApi: mockQueryApi,
          autoQuery: false,
        })
      );

      act(() => {
        result.current.setQueryForm({
          ...result.current.queryForm,
          name: '新商品',
        });
      });

      expect(result.current.queryForm.name).toBe('新商品');
    });
  });
});
