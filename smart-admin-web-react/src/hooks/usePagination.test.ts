/**
 * usePagination Hook Tests
 * usePagination Hook 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePagination } from './usePagination';

describe('usePagination', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('基本功能', () => {
    it('應該初始化默認狀態', () => {
      const { result } = renderHook(() => usePagination());

      expect(result.current.pageNum).toBe(1);
      expect(result.current.pageSize).toBe(10);
      expect(result.current.total).toBe(0);
    });

    it('應該接受自定義默認值', () => {
      const { result } = renderHook(() =>
        usePagination({
          defaultPageNum: 2,
          defaultPageSize: 20,
          total: 100,
        })
      );

      expect(result.current.pageNum).toBe(2);
      expect(result.current.pageSize).toBe(20);
      expect(result.current.total).toBe(100);
    });
  });

  describe('setPageNum 方法', () => {
    it('應該設置頁碼', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageNum(5);
      });

      expect(result.current.pageNum).toBe(5);
    });

    it('應該更新 pagination.current', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageNum(3);
      });

      expect(result.current.pagination.current).toBe(3);
    });
  });

  describe('setPageSize 方法', () => {
    it('應該設置每頁大小', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageSize(20);
      });

      expect(result.current.pageSize).toBe(20);
    });

    it('應該更新 pagination.pageSize', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageSize(50);
      });

      expect(result.current.pagination.pageSize).toBe(50);
    });
  });

  describe('setTotal 方法', () => {
    it('應該設置總條數', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setTotal(100);
      });

      expect(result.current.total).toBe(100);
    });

    it('應該更新 pagination.total', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setTotal(200);
      });

      expect(result.current.pagination.total).toBe(200);
    });
  });

  describe('handlePageChange 方法', () => {
    it('應該同時設置頁碼和每頁大小', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.handlePageChange(3, 20);
      });

      expect(result.current.pageNum).toBe(3);
      expect(result.current.pageSize).toBe(20);
    });

    it('應該調用 onChange 回調', () => {
      const onChange = vi.fn();
      const { result } = renderHook(() => usePagination({ onChange }));

      act(() => {
        result.current.handlePageChange(2, 20);
      });

      expect(onChange).toHaveBeenCalledWith(2, 20);
      expect(onChange).toHaveBeenCalledTimes(1);
    });

    it('應該更新 pagination 對象', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.handlePageChange(4, 50);
      });

      expect(result.current.pagination.current).toBe(4);
      expect(result.current.pagination.pageSize).toBe(50);
    });
  });

  describe('reset 方法', () => {
    it('應該重置到默認頁碼和每頁大小', () => {
      const { result } = renderHook(() =>
        usePagination({
          defaultPageNum: 1,
          defaultPageSize: 10,
        })
      );

      // 先修改狀態
      act(() => {
        result.current.setPageNum(5);
        result.current.setPageSize(50);
      });

      expect(result.current.pageNum).toBe(5);
      expect(result.current.pageSize).toBe(50);

      // 重置
      act(() => {
        result.current.reset();
      });

      expect(result.current.pageNum).toBe(1);
      expect(result.current.pageSize).toBe(10);
    });

    it('應該重置到自定義默認值', () => {
      const { result } = renderHook(() =>
        usePagination({
          defaultPageNum: 2,
          defaultPageSize: 20,
        })
      );

      // 修改狀態
      act(() => {
        result.current.setPageNum(10);
        result.current.setPageSize(100);
      });

      // 重置
      act(() => {
        result.current.reset();
      });

      expect(result.current.pageNum).toBe(2);
      expect(result.current.pageSize).toBe(20);
    });
  });

  describe('pagination 配置', () => {
    it('應該包含 onChange 回調', () => {
      const { result } = renderHook(() => usePagination());

      expect(result.current.pagination.onChange).toBeDefined();
      expect(typeof result.current.pagination.onChange).toBe('function');
    });

    it('應該包含 onShowSizeChange 回調', () => {
      const { result } = renderHook(() => usePagination());

      expect(result.current.pagination.onShowSizeChange).toBeDefined();
      expect(typeof result.current.pagination.onShowSizeChange).toBe('function');
    });

    it('應該默認顯示快速跳轉', () => {
      const { result } = renderHook(() => usePagination());

      expect(result.current.pagination.showQuickJumper).toBe(true);
    });

    it('應該默認顯示每頁大小選擇器', () => {
      const { result } = renderHook(() => usePagination());

      expect(result.current.pagination.showSizeChanger).toBe(true);
    });

    it('應該支持自定義每頁大小選項', () => {
      const { result } = renderHook(() =>
        usePagination({
          pageSizeOptions: [5, 10, 15, 20],
        })
      );

      expect(result.current.pagination.pageSizeOptions).toEqual([5, 10, 15, 20]);
    });

    it('應該默認顯示總數', () => {
      const { result } = renderHook(() =>
        usePagination({
          total: 100,
        })
      );

      expect(result.current.pagination.showTotal).toBeDefined();
      expect(typeof result.current.pagination.showTotal).toBe('function');
    });

    it('應該支持隱藏總數', () => {
      const { result } = renderHook(() =>
        usePagination({
          showTotal: false,
        })
      );

      expect(result.current.pagination.showTotal).toBeUndefined();
    });

    it('應該格式化總數文本', () => {
      const { result } = renderHook(() =>
        usePagination({
          total: 100,
          pageSize: 10,
        })
      );

      const showTotal = result.current.pagination.showTotal as (
        total: number,
        range: [number, number]
      ) => string;

      expect(showTotal(100, [1, 10])).toBe('第 1-10 條 / 共 100 條');
      expect(showTotal(100, [11, 20])).toBe('第 11-20 條 / 共 100 條');
    });
  });

  describe('完整工作流', () => {
    it('應該支持完整的分頁操作流程', () => {
      const onChange = vi.fn();
      const { result } = renderHook(() =>
        usePagination({
          defaultPageNum: 1,
          defaultPageSize: 10,
          total: 100,
          onChange,
        })
      );

      // 1. 初始狀態
      expect(result.current.pageNum).toBe(1);
      expect(result.current.pageSize).toBe(10);
      expect(result.current.total).toBe(100);

      // 2. 設置總數
      act(() => {
        result.current.setTotal(200);
      });
      expect(result.current.total).toBe(200);

      // 3. 切換頁碼
      act(() => {
        result.current.handlePageChange(2, 10);
      });
      expect(result.current.pageNum).toBe(2);
      expect(onChange).toHaveBeenCalledWith(2, 10);

      // 4. 改變每頁大小（通常會重置到第一頁）
      act(() => {
        result.current.handlePageChange(1, 20);
      });
      expect(result.current.pageNum).toBe(1);
      expect(result.current.pageSize).toBe(20);
      expect(onChange).toHaveBeenCalledWith(1, 20);

      // 5. 重置分頁
      act(() => {
        result.current.handlePageChange(5, 50);
      });
      expect(result.current.pageNum).toBe(5);
      expect(result.current.pageSize).toBe(50);

      act(() => {
        result.current.reset();
      });
      expect(result.current.pageNum).toBe(1);
      expect(result.current.pageSize).toBe(10);
    });

    it('應該與 Ant Design Table 集成使用', () => {
      const { result } = renderHook(() =>
        usePagination({
          total: 100,
          defaultPageNum: 1,
          defaultPageSize: 10,
        })
      );

      // 驗證 pagination 配置可以直接傳給 Table
      const tablePagination = result.current.pagination;

      expect(tablePagination).toMatchObject({
        current: 1,
        pageSize: 10,
        total: 100,
        showQuickJumper: true,
        showSizeChanger: true,
      });

      // 模擬 Table onChange
      act(() => {
        tablePagination.onChange?.(2, 10);
      });

      expect(result.current.pageNum).toBe(2);
    });
  });

  describe('邊界情況', () => {
    it('應該處理頁碼為 0 的情況', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageNum(0);
      });

      expect(result.current.pageNum).toBe(0);
    });

    it('應該處理負數頁碼', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageNum(-1);
      });

      expect(result.current.pageNum).toBe(-1);
    });

    it('應該處理超大頁碼', () => {
      const { result } = renderHook(() => usePagination());

      act(() => {
        result.current.setPageNum(999999);
      });

      expect(result.current.pageNum).toBe(999999);
    });

    it('應該處理 total 為 0 的情況', () => {
      const { result } = renderHook(() =>
        usePagination({
          total: 0,
        })
      );

      expect(result.current.total).toBe(0);
      expect(result.current.pagination.total).toBe(0);
    });
  });

  describe('onChange 回調', () => {
    it('應該在沒有提供 onChange 時不報錯', () => {
      const { result } = renderHook(() => usePagination());

      expect(() => {
        act(() => {
          result.current.handlePageChange(2, 20);
        });
      }).not.toThrow();
    });

    it('應該在多次變化時都調用 onChange', () => {
      const onChange = vi.fn();
      const { result } = renderHook(() => usePagination({ onChange }));

      act(() => {
        result.current.handlePageChange(2, 10);
      });
      expect(onChange).toHaveBeenCalledTimes(1);

      act(() => {
        result.current.handlePageChange(3, 10);
      });
      expect(onChange).toHaveBeenCalledTimes(2);

      act(() => {
        result.current.handlePageChange(3, 20);
      });
      expect(onChange).toHaveBeenCalledTimes(3);
    });
  });
});
