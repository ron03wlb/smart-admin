/**
 * useDict Hook
 * 字典數據 Hook
 *
 * 參考：Vue 版本的 useDictStore() actions
 * 提供字典數據獲取、標籤查詢、刷新等功能
 *
 * 使用示例 1：獲取特定字典數據
 * ```tsx
 * import { useDict } from '@/hooks/useDict';
 *
 * function MyComponent() {
 *   // 方式 1：直接獲取字典數據
 *   const goodsPlaceData = useDict('GOODS_PLACE');
 *
 *   return (
 *     <Select
 *       options={goodsPlaceData.map(item => ({
 *         label: item.dataLabel,
 *         value: item.dataValue
 *       }))}
 *     />
 *   );
 * }
 * ```
 *
 * 使用示例 2：獲取字典操作方法
 * ```tsx
 * function AnotherComponent() {
 *   const { getDictLabel, refreshDict, loading } = useDict();
 *
 *   // 獲取字典標籤
 *   const label = getDictLabel('GOODS_PLACE', '1');
 *
 *   return (
 *     <div>
 *       <span>{label}</span>
 *       <Button onClick={refreshDict} loading={loading}>
 *         刷新字典
 *       </Button>
 *     </div>
 *   );
 * }
 * ```
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useCallback } from 'react';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { store } from '@/store';
import {
  fetchAllDictData,
  selectDictDataByCode,
  selectDictLabel as selectDictLabelSelector,
  selectDictLoading,
  selectDictList,
  selectDictLastUpdated,
} from '@/store/slices/dictSlice';
import type { DictDataItem, DictItem } from '@/types/dict';

/**
 * 字典緩存時間（毫秒）
 * 默認 15 分鐘
 */
const DICT_CACHE_DURATION = 15 * 60 * 1000;

/**
 * useDict Hook - Overloaded Signatures
 *
 * 1. useDict(dictCode) - 返回特定字典的數據
 * 2. useDict() - 返回字典操作對象
 */
export function useDict(dictCode: string): DictDataItem[];
export function useDict(): {
  loading: boolean;
  dictList: DictItem[];
  lastUpdated: number | null;
  getDictData: (dictCode: string) => DictDataItem[];
  getDictLabel: (dictCode: string, dataValue: string | number | null | undefined) => string;
  getDictList: () => DictItem[];
  refreshDict: () => Promise<void>;
  isCacheExpired: () => boolean;
  smartRefreshDict: () => Promise<void>;
};

/**
 * useDict Hook Implementation
 */
export function useDict(dictCode?: string) {
  const dispatch = useAppDispatch();

  // 從 Redux 獲取狀態
  const loading = useAppSelector(selectDictLoading);
  const dictList = useAppSelector(selectDictList);
  const lastUpdated = useAppSelector(selectDictLastUpdated);

  // 如果傳入 dictCode，直接返回該字典的數據
  const dictData = useAppSelector(state => (dictCode ? selectDictDataByCode(state, dictCode) : []));

  /**
   * 獲取字典數據
   * 從當前 Redux 狀態中查詢字典數據
   */
  const getDictData = useCallback((code: string): DictDataItem[] => {
    const state = store.getState();
    return selectDictDataByCode(state, code);
  }, []);

  /**
   * 獲取字典標籤
   * 支持單值和多值（逗號分隔）
   */
  const getDictLabel = useCallback(
    (code: string, dataValue: string | number | null | undefined): string => {
      const state = store.getState();
      return selectDictLabelSelector(state, code, dataValue);
    },
    []
  );

  /**
   * 獲取字典列表
   */
  const getDictList = useCallback((): DictItem[] => {
    return dictList;
  }, [dictList]);

  /**
   * 刷新字典數據
   * 調用後端 API 重新獲取所有字典數據
   */
  const refreshDict = useCallback(async (): Promise<void> => {
    await dispatch(fetchAllDictData()).unwrap();
  }, [dispatch]);

  /**
   * 檢查字典緩存是否過期
   */
  const isCacheExpired = useCallback((): boolean => {
    if (!lastUpdated) {
      return true;
    }
    return Date.now() - lastUpdated > DICT_CACHE_DURATION;
  }, [lastUpdated]);

  /**
   * 智能刷新字典
   * 如果緩存過期則刷新，否則跳過
   */
  const smartRefreshDict = useCallback(async (): Promise<void> => {
    if (isCacheExpired()) {
      await refreshDict();
    }
  }, [isCacheExpired, refreshDict]);

  // 如果傳入了 dictCode，直接返回該字典的數據
  if (dictCode) {
    return dictData;
  }

  // 否則返回完整的操作對象
  return {
    loading,
    dictList,
    lastUpdated,
    getDictData,
    getDictLabel,
    getDictList,
    refreshDict,
    isCacheExpired,
    smartRefreshDict,
  };
}

/**
 * useDictLabel Hook
 * 獲取字典標籤的便捷 Hook
 *
 * 使用示例：
 * ```tsx
 * const label = useDictLabel('GOODS_PLACE', '1'); // "中國"
 * ```
 */
export function useDictLabel(
  dictCode: string,
  dataValue: string | number | null | undefined
): string {
  return useAppSelector(state => selectDictLabelSelector(state, dictCode, dataValue));
}
