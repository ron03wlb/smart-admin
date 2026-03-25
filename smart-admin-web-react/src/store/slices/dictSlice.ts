/**
 * Dictionary Redux Slice
 * 字典 Redux Slice
 *
 * 參考：Vue 版本 smart-admin-web/src/store/modules/system/dict.ts
 * 遷移：Pinia Store → Redux Toolkit Slice
 *
 * 狀態管理：
 * - dictList: 字典代碼列表
 * - dictMap: 字典數據映射表（Record<dictCode, DictDataItem[]>）
 * - loading: 加載狀態
 * - error: 錯誤信息
 * - lastUpdated: 最後更新時間（用於緩存策略）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { dictApi } from '@/api/support/dictApi';
import type { DictItem, DictDataItem } from '@/types/dict';
import type { RootState } from '../index';
import { DICT_SPLIT } from '@/constants/support/dictConst';

// ==================== State Interface ====================

interface DictState {
  /**
   * 字典代碼列表
   * 用於顯示所有可用的字典
   */
  dictList: DictItem[];

  /**
   * 字典數據映射表
   * Key: dictCode (字典代碼)
   * Value: DictDataItem[] (該字典的所有數據項)
   *
   * 示例：
   * {
   *   "GOODS_PLACE": [
   *     { dictCode: "GOODS_PLACE", dataValue: "1", dataLabel: "中國" },
   *     { dictCode: "GOODS_PLACE", dataValue: "2", dataLabel: "美國" }
   *   ]
   * }
   */
  dictMap: Record<string, DictDataItem[]>;

  /**
   * 加載狀態
   */
  loading: boolean;

  /**
   * 錯誤信息
   */
  error: string | null;

  /**
   * 最後更新時間戳
   * 用於緩存策略（15 分鐘緩存）
   */
  lastUpdated: number | null;
}

const initialState: DictState = {
  dictList: [],
  dictMap: {},
  loading: false,
  error: null,
  lastUpdated: null,
};

// ==================== Async Thunks ====================

/**
 * 獲取所有字典數據
 * 異步 Thunk：調用後端 API 獲取全部字典數據並初始化 State
 *
 * 參考 Vue 版本：
 * - actions.refreshData() - 調用 API
 * - actions.initData() - 初始化字典數據
 */
export const fetchAllDictData = createAsyncThunk(
  'dict/fetchAllDictData',
  async (_, { rejectWithValue }) => {
    try {
      const response = await dictApi.getAllDictData();

      if (response.ok) {
        return response.data;
      } else {
        return rejectWithValue(response.msg || '獲取字典數據失敗');
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '網絡錯誤';
      return rejectWithValue(message);
    }
  }
);

// ==================== Slice ====================

export const dictSlice = createSlice({
  name: 'dict',
  initialState,
  reducers: {
    /**
     * 清空字典數據
     */
    clearDictData: state => {
      state.dictList = [];
      state.dictMap = {};
      state.lastUpdated = null;
    },

    /**
     * 手動設置字典數據
     * 用於從其他來源初始化字典（例如：從登錄響應中獲取）
     *
     * @param action.payload 字典數據項列表
     */
    initDictData: (state, action: PayloadAction<DictDataItem[]>) => {
      const dictDataList = action.payload;

      // 清空現有數據
      state.dictList = [];
      state.dictMap = {};

      // 初始化字典列表和映射表
      for (const data of dictDataList) {
        // 添加到 dictList（去重）
        if (!state.dictList.some(item => item.dictCode === data.dictCode)) {
          state.dictList.push({
            dictCode: data.dictCode,
            dictName: data.dictName,
            disabledFlag: data.dictDisabledFlag,
          });
        }

        // 添加到 dictMap
        if (!state.dictMap[data.dictCode]) {
          state.dictMap[data.dictCode] = [];
        }
        state.dictMap[data.dictCode].push(data);
      }

      state.lastUpdated = Date.now();
    },
  },
  extraReducers: builder => {
    builder
      .addCase(fetchAllDictData.pending, state => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchAllDictData.fulfilled, (state, action) => {
        state.loading = false;

        const dictDataList = action.payload;

        // 清空現有數據
        state.dictList = [];
        state.dictMap = {};

        // 初始化字典列表和映射表（與 initDictData reducer 相同邏輯）
        for (const data of dictDataList) {
          if (!state.dictList.some(item => item.dictCode === data.dictCode)) {
            state.dictList.push({
              dictCode: data.dictCode,
              dictName: data.dictName,
              disabledFlag: data.dictDisabledFlag,
            });
          }

          if (!state.dictMap[data.dictCode]) {
            state.dictMap[data.dictCode] = [];
          }
          state.dictMap[data.dictCode].push(data);
        }

        state.lastUpdated = Date.now();
      })
      .addCase(fetchAllDictData.rejected, (state, action) => {
        state.loading = false;
        state.error = typeof action.payload === 'string' ? action.payload : '獲取字典數據失敗';
      });
  },
});

// ==================== Selectors ====================

/**
 * 獲取字典列表
 */
export const selectDictList = (state: RootState) => state.dict.dictList;

/**
 * 獲取字典加載狀態
 */
export const selectDictLoading = (state: RootState) => state.dict.loading;

/**
 * 獲取字典錯誤信息
 */
export const selectDictError = (state: RootState) => state.dict.error;

/**
 * 獲取字典最後更新時間
 */
export const selectDictLastUpdated = (state: RootState) => state.dict.lastUpdated;

/**
 * 根據 dictCode 獲取字典數據
 *
 * 參考 Vue 版本：getDictData(dictCode)
 *
 * 使用示例：
 * ```ts
 * const dictData = useAppSelector((state) => selectDictDataByCode(state, 'GOODS_PLACE'));
 * ```
 *
 * @param state Redux State
 * @param dictCode 字典代碼
 * @returns 字典數據項列表
 */
export const selectDictDataByCode = (state: RootState, dictCode: string): DictDataItem[] => {
  if (!dictCode) {
    return [];
  }
  return state.dict.dictMap[dictCode] || [];
};

/**
 * 根據 dictCode 和 dataValue 獲取字典標籤
 *
 * 參考 Vue 版本：getDataLabels(dictCode, dataValue)
 *
 * 支持單值和多值（逗號分隔）：
 * - 單值：dataValue="1" → "選項1"
 * - 多值：dataValue="1,2,3" → "選項1,選項2,選項3"
 * - 數字：dataValue=1 → "選項1" (自動轉換為字符串)
 *
 * 使用示例：
 * ```ts
 * const label = useAppSelector((state) => selectDictLabel(state, 'GOODS_PLACE', '1'));
 * const multiLabel = useAppSelector((state) => selectDictLabel(state, 'GOODS_PLACE', '1,2,3'));
 * ```
 *
 * @param state Redux State
 * @param dictCode 字典代碼
 * @param dataValue 字典值（支持逗號分隔的多個值）
 * @returns 字典標籤（多個值時用逗號分隔）
 */
export const selectDictLabel = (
  state: RootState,
  dictCode: string,
  dataValue: string | number | null | undefined
): string => {
  // 空值檢查
  if (dataValue === null || dataValue === undefined || Number.isNaN(dataValue)) {
    return '';
  }

  // 獲取字典數據
  const dictData = selectDictDataByCode(state, dictCode);
  if (dictData.length === 0) {
    return '';
  }

  // 數字類型特殊處理（轉換為字符串）
  if (typeof dataValue === 'number') {
    const target = dictData.find(item => item.dataValue === String(dataValue));
    return target ? target.dataLabel : '';
  }

  // 字符串類型：支持逗號分隔的多個值
  const valueArray = String(dataValue).split(DICT_SPLIT);
  const result: string[] = [];

  for (const value of valueArray) {
    const target = dictData.find(item => item.dataValue === value);
    if (target) {
      result.push(target.dataLabel);
    }
  }

  return result.join(DICT_SPLIT);
};

// ==================== Exports ====================

export const { clearDictData, initDictData } = dictSlice.actions;

export default dictSlice.reducer;
