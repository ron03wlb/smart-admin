/**
 * usePagination Hook
 * 分頁邏輯 Hook
 *
 * 用法：
 * const pagination = usePagination({
 *   defaultPageNum: 1,
 *   defaultPageSize: 10,
 *   total: 100,
 *   onChange: (pageNum, pageSize) => {
 *     // 處理分頁變化
 *   }
 * });
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { useState, useCallback, useMemo } from 'react';
import type { TablePaginationConfig } from 'antd';

/**
 * 分頁配置
 */
export interface PaginationConfig {
  /** 當前頁碼 */
  pageNum?: number;
  /** 每頁大小 */
  pageSize?: number;
  /** 總條數 */
  total?: number;
}

/**
 * usePagination 選項
 */
export interface UsePaginationOptions {
  /** 默認頁碼 */
  defaultPageNum?: number;
  /** 默認每頁大小 */
  defaultPageSize?: number;
  /** 總條數 */
  total?: number;
  /** 頁碼或每頁大小變化時的回調 */
  onChange?: (pageNum: number, pageSize: number) => void;
  /** 是否顯示快速跳轉 */
  showQuickJumper?: boolean;
  /** 是否顯示每頁大小選擇器 */
  showSizeChanger?: boolean;
  /** 每頁大小選項 */
  pageSizeOptions?: number[];
  /** 是否顯示總數 */
  showTotal?: boolean;
}

/**
 * usePagination 返回值
 */
export interface UsePaginationResult {
  /** 當前頁碼 */
  pageNum: number;
  /** 每頁大小 */
  pageSize: number;
  /** 總條數 */
  total: number;
  /** Ant Design Table 分頁配置 */
  pagination: TablePaginationConfig;
  /** 設置頁碼 */
  setPageNum: (pageNum: number) => void;
  /** 設置每頁大小 */
  setPageSize: (pageSize: number) => void;
  /** 設置總條數 */
  setTotal: (total: number) => void;
  /** 重置分頁到第一頁 */
  reset: () => void;
  /** 頁碼變化處理 */
  handlePageChange: (page: number, pageSize: number) => void;
}

/**
 * 分頁邏輯 Hook
 */
export function usePagination(options: UsePaginationOptions = {}): UsePaginationResult {
  const {
    defaultPageNum = 1,
    defaultPageSize = 10,
    total: initialTotal = 0,
    onChange,
    showQuickJumper = true,
    showSizeChanger = true,
    pageSizeOptions = [10, 20, 50, 100],
    showTotal = true,
  } = options;

  // 當前頁碼
  const [pageNum, setPageNum] = useState(defaultPageNum);
  // 每頁大小
  const [pageSize, setPageSize] = useState(defaultPageSize);
  // 總條數
  const [total, setTotal] = useState(initialTotal);

  /**
   * 頁碼或每頁大小變化處理
   */
  const handlePageChange = useCallback(
    (page: number, size: number) => {
      setPageNum(page);
      setPageSize(size);

      if (onChange) {
        onChange(page, size);
      }
    },
    [onChange]
  );

  /**
   * 重置分頁（回到第一頁）
   */
  const reset = useCallback(() => {
    setPageNum(defaultPageNum);
    setPageSize(defaultPageSize);
  }, [defaultPageNum, defaultPageSize]);

  /**
   * Ant Design Table 分頁配置
   */
  const pagination: TablePaginationConfig = useMemo(
    () => ({
      current: pageNum,
      pageSize,
      total,
      showQuickJumper,
      showSizeChanger,
      pageSizeOptions,
      showTotal: showTotal
        ? (total, range) => `第 ${range[0]}-${range[1]} 條 / 共 ${total} 條`
        : undefined,
      onChange: handlePageChange,
      onShowSizeChange: handlePageChange,
    }),
    [
      pageNum,
      pageSize,
      total,
      showQuickJumper,
      showSizeChanger,
      pageSizeOptions,
      showTotal,
      handlePageChange,
    ]
  );

  return {
    pageNum,
    pageSize,
    total,
    pagination,
    setPageNum,
    setPageSize,
    setTotal,
    reset,
    handlePageChange,
  };
}
